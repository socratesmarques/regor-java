CREATE TABLE users (
 id UUID PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
 vip BOOLEAN NOT NULL DEFAULT FALSE, verified BOOLEAN NOT NULL DEFAULT FALSE,
 coins BIGINT NOT NULL DEFAULT 0 CHECK (coins >= 0), created_at TIMESTAMPTZ NOT NULL,
 CHECK (email = lower(email))
);
CREATE TABLE profiles (
 id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 display_name VARCHAR(80) NOT NULL, username VARCHAR(40) NOT NULL UNIQUE,
 birth_date DATE NOT NULL, location VARCHAR(120) NOT NULL DEFAULT '',
 bio VARCHAR(2000) NOT NULL DEFAULT '', interests VARCHAR(500) NOT NULL DEFAULT '',
 gender VARCHAR(30) NOT NULL DEFAULT '', latitude DOUBLE PRECISION, longitude DOUBLE PRECISION,
 CHECK (latitude BETWEEN -90 AND 90), CHECK (longitude BETWEEN -180 AND 180),
 CHECK ((latitude IS NULL) = (longitude IS NULL))
);
CREATE TABLE photos (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 content BYTEA NOT NULL, media_type VARCHAR(30) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 CHECK (media_type IN ('image/jpeg','image/png')), CHECK (octet_length(content) <= 5242880)
);
CREATE INDEX photos_user_idx ON photos(user_id,created_at);
CREATE TABLE user_preferences (
 id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 min_age INTEGER NOT NULL DEFAULT 18, max_age INTEGER NOT NULL DEFAULT 60,
 max_distance_km INTEGER NOT NULL DEFAULT 100, interest VARCHAR(80) NOT NULL DEFAULT '',
 gender VARCHAR(30) NOT NULL DEFAULT '', location VARCHAR(120) NOT NULL DEFAULT '',
 matches_notif BOOLEAN NOT NULL DEFAULT TRUE, messages_notif BOOLEAN NOT NULL DEFAULT TRUE,
 show_distance BOOLEAN NOT NULL DEFAULT TRUE, show_online BOOLEAN NOT NULL DEFAULT TRUE,
 discoverable BOOLEAN NOT NULL DEFAULT TRUE, theme VARCHAR(10) NOT NULL DEFAULT 'escuro',
 language VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
 CHECK (min_age >= 18 AND max_age <= 120 AND min_age <= max_age),
 CHECK (max_distance_km BETWEEN 1 AND 20000), CHECK (theme IN ('escuro','claro'))
);
CREATE TABLE swipes (
 id UUID PRIMARY KEY, actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 target_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 kind VARCHAR(20) NOT NULL CHECK (kind IN ('LIKE','DISLIKE','SUPERLIKE')), created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(actor_id,target_id), CHECK (actor_id <> target_id)
);
CREATE INDEX swipes_target_idx ON swipes(target_id,kind);
CREATE TABLE matches (
 id UUID PRIMARY KEY, user_low UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 user_high UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(user_low,user_high), CHECK(user_low < user_high)
);
CREATE INDEX matches_high_idx ON matches(user_high);
CREATE TABLE chats (
 id UUID PRIMARY KEY, match_id UUID NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE chat_participants (
 id UUID PRIMARY KEY, chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, UNIQUE(chat_id,user_id)
);
CREATE INDEX participants_user_idx ON chat_participants(user_id);
CREATE TABLE messages (
 id UUID PRIMARY KEY, chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
 sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 client_id UUID NOT NULL, content VARCHAR(4000) NOT NULL CHECK(length(trim(content)) > 0),
 created_at TIMESTAMPTZ NOT NULL, UNIQUE(sender_id,client_id),
 FOREIGN KEY(chat_id,sender_id) REFERENCES chat_participants(chat_id,user_id)
);
CREATE INDEX messages_chat_idx ON messages(chat_id,created_at,id);
CREATE TABLE notifications (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 type VARCHAR(20) NOT NULL CHECK(type IN ('LIKE','SUPERLIKE','MATCH','MESSAGE')),
 text VARCHAR(250) NOT NULL, chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
 read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX notifications_user_idx ON notifications(user_id,created_at);
