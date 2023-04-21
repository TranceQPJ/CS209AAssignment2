CREATE TABLE "user"
(
    user_id  SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50)        NOT NULL
);

CREATE TABLE chat
(
    chat_id   SERIAL PRIMARY KEY,
    chat_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_user
(
    chat_id INTEGER REFERENCES chat (chat_id),
    user_id INTEGER REFERENCES "user" (user_id),
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE chatMessage
(
    message_id      SERIAL PRIMARY KEY,
    message_content TEXT      NOT NULL,
    chat_id         INTEGER REFERENCES chat (chat_id),
    sender_id       INTEGER REFERENCES "user" (user_id),
    sent_time       TIMESTAMP NOT NULL DEFAULT NOW()
);

