# Nestio

A PG management app for any owner. Happy Nest is a separate project and is not changed by this folder.

## Where it is

`C:\Users\duggi\nestio`

- `frontend` — the website
- `backend` — the API

## First open

1. Create a PostgreSQL database named `nestio`.
2. Start the backend from `backend` (port 8080).
3. In `frontend`, run `npm install` once, then `npm run dev`.
4. Open the site. The first screen asks for a username, password, PG name, number of floors, rooms per floor, and whether each room is single, double, triple, or four sharing.
5. After that, sign in and use residents, payments, deposit, refund, temporary stays, the AI assistant, and notifications the same way as Happy Nest.

The AI assistant needs `AI_API_KEY` on the backend. Phone alerts need the site on https and **Turn on phone alerts** after adding it to the home screen.
