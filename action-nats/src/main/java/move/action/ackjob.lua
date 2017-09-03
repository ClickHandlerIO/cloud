-- ACKJOB - Acknowledge the job was received
-- EVALSHA sha1 jobId status

local job = redis.call("HGETALL", 'j/' .. KEYS[1])
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

