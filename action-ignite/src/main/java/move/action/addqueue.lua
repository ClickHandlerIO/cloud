-- ADDQUEUE -
-- EVALSHA sha1 1 queueName

local job = redis.call("HGETALL", KEYS[1])
if job == nil then
    return -1
end

-- Did it complete?
if status == 1 then
    -- Try to remove job

    -- Next job

    -- Notify worker
else
    -- Try to requeue job
end

