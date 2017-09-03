-- ADDJOB {queueId} sent message

local queueKey = KEYS[1]
-- Get Queue props
local queue = redis.call("HMGET", queueKey, "a", "e", "f", "g", "h", "j")

-- Ensure Queue exists
if queue == nil then
    return -1
end

-- a|1 = Job Counter
--local lastJobID = tonumber(queue[1])
-- e|2 = Target Execution Time
local targetExecutionTime = tonumber(queue[2])
-- f|3 = Max Execution Time
local maxExecutionTime = tonumber(queue[3])
-- g|4 = Max Backlog Size
local maxBacklogSize = tonumber(queue[4])
-- h|5 = Max Message Size
local maxMessageSize = tonumber(queue[5])
-- j|6 = Max Deliveries
local maxDeliveries = tonumber(queue[6])


-- Set message
local message = ARGV[1]
local messageLength = string.len(message)

-- Ensure message isn't too big

local backlogKey = queueKey .. ":b"

-- Ensure Backlog isn't too long
if maxBacklogSize > 0 and redis.call("LLEN", backlogKey) > maxBacklogSize then
    return -2
end

-- Get current time
local _time = redis.call("TIME")
local millis = _time[1] * 1000 + (_time[2] / 1000)

-- Create JobID
local jobNum = redis.call("HINCRBY", queueKey, "a")
local jobKey = queueKey .. ":" .. jobNum

local job = {
    id = jobNum,
    status = 1,
    sent = millis,
    received = millis,
    startAt = 0,
    dispatched = 0,
    expires = 0,
    ack = 0,
    tryCount = 1,
    deDupId = "0",
    body = 0xff
}

-- Save Job
redis.call("HMSET", jobKey, "a", 1, "b", 0, "")

-- Try to find worker to service the job
local nextWorkerResult = redis.call("ZRANGE", queueKey .. ":w", 0, 1)

if (nextWorkerResult == nil) then
    -- Enqueue Job
    redis.call("RPUSH", backlogKey, jobKey)
    return 2
end

local availableMemory = redis.call("HGET", queueKey .. ":w:" .. nextWorkerResult[1], "e")

--if availableMemory <

-- Maybe process delayed
local delayCount = redis.call("ZCARD", queueKey .. ":d")

return 0

