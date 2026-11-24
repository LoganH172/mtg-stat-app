# Commander Ledger gRPC contracts

The Java backend generates its message classes and service stubs from this
directory. To generate Android stubs on the current Android branch, copy the
new `card.proto`, `match.proto`, and `stats.proto` files into
`android/app/src/main/proto/` alongside its existing `auth.proto`. The message
and service package stays `commander.v1`.

After registration or login, the client sends `authorization: Bearer <token>`
as gRPC metadata for the match, card, and stats calls. The backend resolves
the caller from the session. A missing, invalid, or expired session returns
`UNAUTHENTICATED`.

## Recording a match across devices

The creator calls `CreateMatch` and gets a match ID and room code. Other players
join with the code. An authenticated participant can read the current match
with `GetMatch` and save their commander with `SelectCommander`. If a match
belongs to a playgroup, the backend checks playgroup membership at creation
and join. Unaffiliated matches omit the playgroup ID.

The creator submits the result once every participant has selected a commander.
The winner and each Losing Card entry must refer to participants in that
match. The Most Valuable Card applies to the winner, and each Losing Card
applies to one eliminated player. Both card selections can be omitted.

A phone that loses its connection after preparing a result can keep the
submission locally and retry with the same match ID when it reconnects.
The backend uses the match ID to recognize duplicate result submissions.
Here is how the server would handle two submissions arriving at the same time.

```text
Begin a database transaction and lock the match record.
Confirm that the signed-in player created the match.
If the match already has a result:
    If the winner and card selections match, end the transaction and return
    the completed match.
    Otherwise, roll back and reject the conflicting submission.
Check the participants, their commanders, and any selected cards.
Save the winner, card selections, and completion time together.
Commit the transaction and return the completed match.
```

Losing Card entries are compared by player, so their order does not affect a
retry. The server scopes each create request UUID to the signed-in player.
An identical retry returns the original match. Reusing that UUID for a
different playgroup returns `ALREADY_EXISTS`. Joining an open match again
returns its current state without duplicating membership. The service returns
`INVALID_ARGUMENT` for malformed inputs, `PERMISSION_DENIED` when the caller
lacks access, and `FAILED_PRECONDITION` when the match state prevents the
requested change.

## History, statistics, and card prices

- History lists the signed-in player's completed matches in reverse completion
  order, with match ID breaking timestamp ties. The first result supplies
  the last match and the player's most recently used commander for the
  dashboard. History and leaderboard page tokens are opaque.
- Profile statistics count only completed matches and group each record by
  the commander's full color identity. A colorless commander has an empty
  color list. The shared leaderboard orders players with completed matches
  by wins, then win percentage, then player ID to break ties.
- `SearchCards` streams bounded Scryfall results, with an optional
  commander-only filter. `GetCard` identifies the exact printing and can
  request a specific finish. Android can display the amount, currency, and
  retrieval time when pricing is present. A missing Scryfall price leaves
  the price field absent. An upstream outage preventing lookup returns
  `UNAVAILABLE`.

The Java server runs the greeting service and supports registration and login
when MySQL is configured. See the [server README](../server/README.md) for local
setup. The match, card, and stats files define the interfaces for service
implementation. The current MySQL schema can
represent a completed match's winner and standout cards through
`game_members`, but it needs persistence for room codes, in-progress
membership, create request IDs, and match completion before it can support
the full match flow.
