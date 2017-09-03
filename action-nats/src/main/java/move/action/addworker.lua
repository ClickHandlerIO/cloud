-- Add a Worker
-- EVALSHA sha1 uid

-- Is the worker already registered?
if redis.call("EXISTS", "w:u:" .. ARGV[1]) == 1 then

end