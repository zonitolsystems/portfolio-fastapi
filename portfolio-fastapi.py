import tensorflow as tf
import keras
import pandas as pd
from janome.tokenizer import Tokenizer
import MeCab

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
from sklearn.model_selection import train_test_split

from fastapi import FastAPI, APIRouter, HTTPException, Header, WebSocket, WebSocketDisconnect, Depends
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import firebase_admin
from firebase_admin import auth, credentials, initialize_app
import uvicorn

from dotenv import load_dotenv
from openai import AsyncOpenAI

import unicodedata
import re
import numpy as np
import os
import sys
import io
import time
import csv
import logging
from logging.handlers import TimedRotatingFileHandler
import json
import datetime
import subprocess
import tempfile
import platform
import ctypes

from sqlalchemy import create_engine, func, cast, Date, Column, Integer, String, Text, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import Session, sessionmaker, relationship, declarative_base
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.future import select

import urllib.parse
from pydantic import BaseModel
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger

os.environ["GOOGLE_CLOUD_AUTH_TICK_OFFSET"] = "600"

def setup_logger():
    # ログのフォーマット設定
    formatter = logging.Formatter(
        '%(asctime)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

    # 1日ごとにファイルを切り替える設定
    file_handler = TimedRotatingFileHandler(
        filename="server.log",
        when="midnight",     # 毎日深夜0時に切り替え
        interval=1,          # 1日おき
        backupCount=36000,      # 過去30日分だけ保存し、古いものは自動削除
        encoding='utf-8'
    )
    # 切り替わった後のファイル名に日付を入れる設定
    file_handler.suffix = "%Y-%m-%d" 
    file_handler.setFormatter(formatter)

    # コンソール（ターミナル）出力用の設定
    stream_handler = logging.StreamHandler(sys.stdout)
    stream_handler.setFormatter(formatter)

    # ロガーの作成
    logger = logging.getLogger("my_app")
    logger.setLevel(logging.INFO)
    logger.addHandler(file_handler)
    logger.addHandler(stream_handler)

    return logger

# インスタンス化
logger = setup_logger()

# .envファイルから環境変数を読み込む
load_dotenv()

# プログラム内では変数として扱う
raw_password = os.getenv("DB_PASSWORD")
encoded_password = urllib.parse.quote_plus(raw_password)

# os.getenv で取得する
client = AsyncOpenAI(api_key=os.getenv("OPENAI_API_KEY"))

async def backup_mysql_job():
    # 保存先ディレクトリの作成
    backup_dir = "./backup_sql"
    if not os.path.exists(backup_dir):
        os.makedirs(backup_dir)

    # ファイル名に日付を入れる
    today = datetime.date.today().strftime("%Y-%m-%d")
    backup_file = os.path.join(backup_dir, f"backup_{today}.sql")

    # パスワードなどを一時的な設定ファイルに書き出す
    config_content = f"""
    [client]
    user=app_user
    password={os.getenv("DB_PASSWORD")}
    """

    # ユーザー専用の一時ディレクトリに作成
    fd, temp_path = tempfile.mkstemp(suffix=".cnf", text=True)
    try:
        with os.fdopen(fd, 'w') as f:
            f.write(config_content)
    except Exception as e:
        logger.error(f"バックアップ一時ディレクトリエラー: {e}")

    # mysqldumpコマンドの組み立て
    cmd = [
        "mysqldump",
        f"--defaults-extra-file={temp_path}",
        "app01_db"
    ]

    try:
        with open(backup_file, "w") as f:
            process = subprocess.Popen(cmd, stdout=f, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate()

        if process.returncode == 0:
            logger.info(f"MySQLバックアップ成功: {backup_file}")
        else:
            logger.error(f"MySQLバックアップ失敗: {stderr.decode()}")
    except Exception as e:
        logger.error(f"バックアップ実行エラー: {e}")
    
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)

# URLを組み立てる
SQLALCHEMY_DATABASE_URL = f"mysql+aiomysql://app_user:{encoded_password}@localhost:3306/app01_db"

# 非同期エンジンの作成
engine = create_async_engine(SQLALCHEMY_DATABASE_URL, echo=True)

# 非同期セッションの作成
AsyncSessionLocal = sessionmaker(
    engine, class_=AsyncSession, expire_on_commit=False
)

Base = declarative_base()

# 依存注入用の非同期ジェネレータ
async def get_db():
    async with AsyncSessionLocal() as session:
        yield session

class User(Base):
    __tablename__ = "users"

    # MySQL 内部で管理する数値ID
    id = Column(Integer, primary_key=True, index=True)
    
    # Firebase から渡される一意の識別子
    f_uid = Column(String(255), unique=True, index=True, nullable=False)
    
    # ユーザーのメールアドレス
    email = Column(String(255), unique=True, index=True, nullable=False)
    
    # 会員資格カラムを追加
    membership = Column(String(50), default="Free User") 

    # 有効期限を管理
    membership_expires_at = Column(DateTime, nullable=True)

    # 作成日時
    created_at = Column(DateTime, default=datetime.datetime.now())

    devices = relationship("UserDevice", back_populates="owner")

    # ChatHistory とのリレーション
    messages = relationship("ChatHistory", back_populates="owner")

async def get_or_create_user(db: AsyncSession, f_uid: str, email: str, membership: str = None):
    # 非同期クエリの実行
    result = await db.execute(select(User).filter(User.f_uid == f_uid))
    user = result.scalars().first()
    
    if not user:
        # 存在しない場合は新しく作成
        logger.info(f"新規ユーザー作成: {email}")
        user = User(
            f_uid=f_uid,
            email=email,
            membership=membership or "free"
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)
    
    else:
        # 既存ユーザーの情報更新チェック
        updated = False
        if user.email != email:
            user.email = email
            updated = True
        
        # 会員資格が変わっている場合に更新
        if membership and user.membership != membership:
            user.membership = membership
            updated = True
            
        if updated:
            await db.commit()
            await db.refresh(user)

    return user

class UserDevice(Base):
    __tablename__ = "user_devices"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String(250), ForeignKey("users.f_uid"), index=True)
    device_id = Column(String(255)) # スマホから送られてくる固有ID
    last_login_at = Column(DateTime, default=datetime.datetime.now, onupdate=datetime.datetime.now)

    # ユーザーモデルとの紐付け
    owner = relationship("User", back_populates="devices")

async def cleanup_old_devices_job():
    async with AsyncSessionLocal() as db:
        try:
            # 現在から30日前を計算
            threshold = datetime.datetime.now() - datetime.timedelta(days=30)
            
            # 条件に合うレコードを削除
            query = delete(UserDevice).where(UserDevice.last_login_at < threshold)
            result = await db.execute(query)
            await db.commit()
            
            logger.info(f"古いデバイス情報を削除しました。削除件数: {result.rowcount}")
        except Exception as e:
            await db.rollback()
            logger.error(f"デバイスクリーンアップ中にエラーが発生しました: {e}")

class ChatHistory(Base):
    __tablename__ = "chat_histories"

    id = Column(Integer, primary_key=True, index=True)

    user_id = Column(String(250), ForeignKey("users.f_uid"))

    app = Column(String(50))
    role = Column(String(50))  # "user" or "assistant"
    content = Column(Text)     # メッセージ内容
    created_at = Column(DateTime, default=datetime.datetime.now()) # 送信日時

    # User モデルへの逆参照
    owner = relationship("User", back_populates="messages")

async def init_models():
    async with engine.begin() as conn:
        # テーブル作成
        await conn.run_sync(Base.metadata.create_all)

# 履歴を保存する関数
async def save_message(db: AsyncSession, user_id: str, app: str, role: str, content: str):
    try:
        db_message = ChatHistory(
            user_id=user_id, 
            app=app, 
            role=role, 
            content=content
        )
        db.add(db_message)
        await db.commit()
        logger.info(f"DB保存成功: {role}")
    except Exception as e:
        await db.rollback()
        logger.error(f"DB保存失敗: {e}")

# 会員資格のみを更新する関数
async def update_user_membership(db: AsyncSession, f_uid: str, new_plan: str):
    result = await db.execute(select(User).filter(User.f_uid == f_uid))
    user = result.scalars().first()
    
    if user:
        user.membership = new_plan
        await db.commit()
        await db.refresh(user)
        return True
    return False

async def get_recent_history(db: AsyncSession, user_id: str, limit: int = 50):
    result = await db.execute(
        select(ChatHistory)
        .filter(ChatHistory.user_id == user_id)
        .order_by(ChatHistory.id.desc())
        .limit(limit)
    )
    rows = result.scalars().all()
    return [{"role": r.role, "content": r.content} for r in reversed(rows)]

# ファイルの絶対パスを取得
current_dir = os.path.dirname(os.path.abspath(__file__))
key_path = os.path.join(current_dir, 'serviceAccountKey.json')

# ユーザーごとの履歴を保持する辞書
chat_histories = {}

# 初期化
cred = credentials.Certificate(key_path)
firebase_admin.initialize_app(cred)

@asynccontextmanager
async def lifespan(_app: FastAPI):
    # データベースの準備
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    logger.info("Database tables verified")

    # スケジューラの初期化
    from pytz import timezone
    scheduler = AsyncIOScheduler(timezone=timezone('Asia/Tokyo'))
    
    # ジョブの追加
    scheduler.add_job(
        aggregate_daily_stats_job, 
        CronTrigger(hour=6, minute=0), # 実行したい時間
        id="daily_stats_task"
    )

    # バックアップジョブを追加
    scheduler.add_job(
        backup_mysql_job,
        CronTrigger(hour=6, minute=30),
        id="mysql_backup_task"
    )

    # デバイスの自動削除ジョブを追加
    scheduler.add_job(
        cleanup_old_devices_job, 
        CronTrigger(hour=5, minute=30), 
        id="device_cleanup_task"
    )

    # スケジューラ開始
    scheduler.start()
    logger.info("Scheduler started (Daily stats at 10:46 JST)")
    
    yield  # アプリ稼働
    
    # アプリ終了時の処理
    scheduler.shutdown()
    await engine.dispose()
    logger.info("Scheduler and Database connection closed")

# lifespanを引数に渡す
app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/register")
async def register_user(authorization: str = Header(None)):
    id_token = authorization.split("Bearer ")[1]
    try:
        # トークンを検証
        decoded_token = auth.verify_id_token(id_token)
        
        # Firebaseが発行した一意のID
        uid = decoded_token['uid']
        # ユーザーが入力したメールアドレス
        email = decoded_token['email']
        logger.info("新規登録: ",email)

        return {"status": "success", "uid": uid}
    except Exception as e:
        raise HTTPException(status_code=401, detail="認証エラー")

@app.post("/login")
async def login(request_data: dict, authorization: str = Header(None), db: AsyncSession = Depends(get_db)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid token header")

    id_token = authorization.split("Bearer ")[1]

    try:
        # Firebaseでトークンを検証
        decoded_token = auth.verify_id_token(id_token)
        
        f_uid = decoded_token['uid']
        device_id = request_data.get("device_id")
        email = decoded_token.get('email')
        client_membership = request_data.get("membership")

        if device_id:

            # このデバイスが既にこのユーザーに登録されているかチェック
            result = await db.execute(
                select(UserDevice).filter(
                    UserDevice.user_id == f_uid, 
                    UserDevice.device_id == device_id
                )
            )
            is_registered = result.scalars().first()

            if is_registered:
                # 既に登録済みの場合は、時刻を更新
                is_registered.last_login_at = datetime.datetime.now()
                await db.commit()
            else:
                # 未登録デバイスの場合、現在の登録台数をカウント
                count_result = await db.execute(
                    select(func.count(UserDevice.id)).filter(UserDevice.user_id == f_uid)
                )
                registered_count = count_result.scalar()

                if registered_count >= 6:
                    # 既に3台登録されている場合はエラー
                    raise HTTPException(
                        status_code=403, 
                        detail="Many login"
                    )

                # 制限内であれば新しいデバイスを登録
                new_device = UserDevice(user_id=f_uid, device_id=device_id)
                db.add(new_device)
                await db.commit()
                logger.info(f"新デバイス登録: {f_uid} - {device_id}")

        # MySQL の処理
        user = await get_or_create_user(db, f_uid, email, client_membership)

        # 過去の履歴を取得
        history = await get_recent_history(db, f_uid, limit=50)
        
        logger.info(f"ログイン: {email} (UID: {f_uid})")
    
        return {
            "status": "success",
            "user_id": f_uid,  # これを Android に返す
            "membership": user.membership,
            "history": history,
            "message": "Login allowed"
        }

    except Exception as e:
        # トークンが期限切れ、または不正な場合
        logger.error(f"トークン検証失敗の詳細: {str(e)}")
        raise HTTPException(status_code=401, detail=f"Token verification failed: {str(e)}")

@app.post("/logout")
async def logout(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Token missing")

    id_token = authorization.split("Bearer ")[1]
    try:
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        email = decoded_token.get('email')
        
        logger.info(f"ログアウト: {email}")
        
        return {"status": "success", "message": "Logged out successfully"}
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")

def delete_user_from_server(uid):
    try:
        # Firebase Authからユーザーを削除
        auth.delete_user(uid)
        
        logger.info(f"アカウント削除: {uid}")
        return True
    except Exception as e:
        logger.info(f"アカウント削除エラー: {e}")
        return False

@app.post("/delete-account")
async def delete_account(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Token missing")

    id_token = authorization.split("Bearer ")[1]
    try:
        # トークンを検証してUIDを特定
        decoded_token = auth.verify_id_token(id_token)
        uid = decoded_token['uid']
        email = decoded_token.get('email')
        
        # Firebase Authからユーザーを削除
        auth.delete_user(uid)
        
        logger.info(f"アカウント削除: {email} (uid: {uid})")
        return {"status": "success", "message": "Account deleted by server"}
        
    except Exception as e:
        logger.info(f"アカウント削除エラー: {e}")
        raise HTTPException(status_code=500, detail="Internal Server Error")

class EmailUpdateRequest(BaseModel):
    f_uid: str
    new_email: str

@app.put("/user/update-email")
async def update_email(request: EmailUpdateRequest, db: AsyncSession = Depends(get_db)):
    # まず該当ユーザーを検索
    result = await db.execute(select(User).filter(User.f_uid == request.f_uid))
    user = result.scalars().first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # メールの重複チェック
    check_result = await db.execute(select(User).filter(User.email == request.new_email))
    existing_user = check_result.scalars().first()
    if existing_user and existing_user.f_uid != request.f_uid:
        raise HTTPException(status_code=400, detail="Email already in use")

    # データを更新
    user.email = request.new_email
    
    try:
        await db.commit()  # 変更を確定
    except Exception as e:
        await db.rollback() # 失敗した場合はロールバック
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    
    return {"status": "success", "updated_email": user.email}
    
@app.get("/user/profile/{f_uid}")
async def get_user_profile(f_uid: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).filter(User.f_uid == f_uid))
    user = result.scalars().first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return {
        "email": user.email,
        "membership": user.membership,
        "membership_expires_at": user.membership_expires_at.isoformat() if user.membership_expires_at else None
    }
class Inquiry(Base):
    __tablename__ = "inquiries"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String(255))
    app = Column(String(255))
    subject = Column(Text)
    message = Column(Text)
    created_at = Column(DateTime, default=datetime.datetime.now())

@app.post("/contact")
async def create_inquiry(data: dict, db: AsyncSession = Depends(get_db)):
    new_inquiry = Inquiry(
        user_id=data.get("user_id"),
        app=data.get("app"),
        subject=data.get("subject"),
        message=data.get("message")
    )
    db.add(new_inquiry)
    await db.commit()
    return {"status": "success"}

class UsageLog(Base):
    __tablename__ = "usage_logs"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(String(250), ForeignKey("users.f_uid"))
    app = Column(String(50))
    from_lang = Column(String(50))
    to_lang = Column(String(50))
    screen_type = Column(String(50)) # "translation" or "chat"
    created_at = Column(DateTime, default=datetime.datetime.now())

class DailyStat(Base):
    __tablename__ = "daily_stats"
    id = Column(Integer, primary_key=True, index=True)
    target_date = Column(Date, index=True) # 統計対象の日
    user_id = Column(String(250))
    app = Column(String(50))
    from_lang = Column(String(50))
    to_lang = Column(String(50))
    screen_type = Column(String(50))
    exec_count = Column(Integer, default=0) # 実行回数

async def record_usage(db, f_uid, app, from_lang, to_lang, screen):
    new_log = UsageLog(
        user_id=f_uid,
        app = app,
        from_lang=from_lang,
        to_lang=to_lang,
        screen_type=screen
    )
    db.add(new_log)
    await db.commit()

# 昨日の統計を生成する関数
async def aggregate_daily_stats(db: AsyncSession):
    # 昨日の日付を取得（UTCベースで前日を計算）
    yesterday = datetime.date.today() - datetime.timedelta(days=1)

    # ログテーブルから、ユーザー・アプリ・言語・画面ごとに件数を集計
    query = (
        select(
            UsageLog.user_id,
            UsageLog.app,
            UsageLog.from_lang,
            UsageLog.to_lang,
            UsageLog.screen_type,
            func.count(UsageLog.id).label("count")
        )
        .filter(cast(UsageLog.created_at, Date) == yesterday)
        .group_by(
            UsageLog.user_id, 
            UsageLog.app, 
            UsageLog.from_lang, 
            UsageLog.to_lang, 
            UsageLog.screen_type
        )
    )
    
    result = await db.execute(query)
    rows = result.all()

    if not rows:
        return

    # 集計結果を DailyStat テーブルに保存
    for row in rows:
        stat = DailyStat(
            target_date=yesterday,
            user_id=row.user_id,
            app=row.app,
            from_lang=row.from_lang,
            to_lang=row.to_lang,
            screen_type=row.screen_type,
            exec_count=row.count
        )
        db.add(stat)
    
    try:
        await db.commit()
    except Exception as e:
        await db.rollback()

# スケジューラから呼ばれるラッパー関数
async def aggregate_daily_stats_job():
    async with AsyncSessionLocal() as db:
        try:
            await aggregate_daily_stats(db)
        except Exception as e:
            logger.error(f"定期ジョブ実行中に予期せぬエラーが発生しました: {e}")

async def get_translation(text: str, input_lang: str = "English", target_lang: str = "English") -> str:
    try:
        # gpt-4o-mini を使用してテキストを翻訳
        response = await client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "system", 
                    "content": f"You are a professional translator. Translate the following text from {input_lang} into {target_lang}. Output only the translated text without any explanations or extra words."
                },
                {
                    "role": "user", 
                    "content": text
                }
            ],
            temperature=0.3,
        )
        
        # 翻訳結果の取得
        translated_text = response.choices[0].message.content.strip()
        return translated_text

    except Exception as e:
        logger.info(f"翻訳エラー: {e}")
        return "Translation Error"

async def get_chat(text: str, history: list, input_lang: str = "English", target_lang: str = "English") -> str:
    try:
        # システムプロンプト：翻訳ではなく「チャット」として振る舞うよう指示
        system_message = {
            "role": "system",
            "content": f"You are a helpful assistant. Please communicate in {target_lang}. Context: The user speaks {input_lang}."
        }
        
        # 直近50件の履歴のみを抽出
        recent_context = history[-50:] if len(history) > 50 else history
        
        messages = [system_message] + recent_context + [{"role": "user", "content": text}]

        response = await client.chat.completions.create(
            model="gpt-4o-mini",
            messages=messages,
            temperature=0.7,
        )
        
        return response.choices[0].message.content.strip()
    except Exception as e:
        logger.info(f"チャット応答エラー: {e}")
        return "Sorry, I encountered an error."

@app.websocket("/ws/chat")
async def websocket_endpoint(websocket: WebSocket):
    # 接続の承認
    await websocket.accept()
    logger.info("WebSocket接続が承認されました")
    
    current_user_id = None
    
    try:
        while True:
            # クライアントからのデータ受信
            data = await websocket.receive_text()
            
            try:
                request_data = json.loads(data)
            except json.JSONDecodeError:
                logger.error("JSONパースエラー")
                continue

            # メッセージ処理の間だけDBセッションを開始
            async with AsyncSessionLocal() as db:
                received_uid = request_data.get("user_id")
                u_id = int(received_uid) if str(received_uid).isdigit() else 1
                app_id = request_data.get("app")
                
                # ユーザーの切り替えと履歴のロード
                if received_uid and current_user_id != received_uid:
                    current_user_id = received_uid
                    chat_histories[current_user_id] = await get_recent_history(db, current_user_id, limit=50)
                    logger.info(f"ユーザー {current_user_id}: 履歴をロードしました")
                
                # --- 初期メッセージのリクエスト判定 ---
                if request_data.get("type") == "initial_greeting":
                    target_lang = request_data.get("to")
                    greeting_prompt = f"Say 'How are you feeling now?' in {target_lang}. Output only the translation."
                    result = await get_translation(greeting_prompt, "English", target_lang)
                    await websocket.send_text(result)
                    logger.info(f"送信(初期挨拶): {current_user_id}, {result}")
                    await save_message(db, current_user_id, app_id, "assistant", result)
                    history = chat_histories.get(current_user_id, [])
                    chat_histories[current_user_id].append({"role": "assistant", "content": result})
                    continue 

                # 通常の翻訳・チャット処理
                is_mode_val = request_data.get("isMode")
                istranslation = False if str(is_mode_val).lower() == 'false' else bool(is_mode_val)
                source_lang = request_data.get("from")
                target_lang = request_data.get("to")
                text = request_data.get("text")

                if istranslation:
                    logger.info(f"受信メッセージ: 翻訳,{current_user_id}, {source_lang},{target_lang},{text}")
                    result = await get_translation(text, source_lang, target_lang)
                    logger.info(f"送信メッセージ: {current_user_id}, {result}")
                    await record_usage(db, current_user_id, app_id, source_lang, target_lang, "translate")
                else:
                    # チャットモード
                    logger.info(f"受信メッセージ: チャット,{current_user_id}, {source_lang},{target_lang},{text}")
                    await save_message(db, current_user_id, app_id, "user", text)
                    result = await get_chat(text, history, source_lang, target_lang)
                    await save_message(db, current_user_id, app_id, "assistant", result)
                    await record_usage(db, current_user_id, app_id, source_lang, target_lang, "chat")
                    
                    # 履歴を更新
                    chat_histories[current_user_id].append({"role": "user", "content": text})
                    chat_histories[current_user_id].append({"role": "assistant", "content": result})
                    
                    logger.info(f"送信メッセージ: {current_user_id}, {result}")
                    
                    if len(chat_histories[current_user_id]) > 50:
                        chat_histories[current_user_id] = chat_histories[current_user_id][-50:]

                # 結果を送信
                await websocket.send_text(result)

    except WebSocketDisconnect:
        logger.info(f"ユーザー {current_user_id} が切断されました")
    except Exception as e:
        logger.error(f"予期せぬエラー: {e}", exc_info=True)
    finally:
        # メモリ節約
        if current_user_id in chat_histories:
            del chat_histories[current_user_id]

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
