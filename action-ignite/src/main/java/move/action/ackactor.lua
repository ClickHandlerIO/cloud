-- ACKACTOR - Acknowledge Actor
-- EVALSHA sha1 actorKey status

local time = redis.call("TIME")[1]

return time