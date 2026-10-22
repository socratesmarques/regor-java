ALTER TABLE users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
CREATE FUNCTION check_chat_participant() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS (
   SELECT 1 FROM chats c JOIN matches m ON m.id=c.match_id
   WHERE c.id=NEW.chat_id AND NEW.user_id IN (m.user_low,m.user_high)
 ) THEN RAISE EXCEPTION 'Participant must belong to the chat match' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER chat_participant_match_guard BEFORE INSERT OR UPDATE ON chat_participants
FOR EACH ROW EXECUTE FUNCTION check_chat_participant();
