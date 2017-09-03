-- Add a Worker
-- EVALSHA sha1 {queueId} workerId

local queue = redis.call("HMGET", KEYS[1], "s")
if queue == nil then
    return -1
end

return 0