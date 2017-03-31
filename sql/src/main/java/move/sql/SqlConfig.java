package move.sql;

/**
 *
 */
public class SqlConfig {
    private String name = "CORE";
    private String url = "jdbc:h2:mem:move;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE;";
    private String catalog = "";
    private String schema = "public";
    private String user = "root";
    private String password = "passme";
    private String readUrl = "";
    private String readUser = "";
    private String readPassword = "";
    private int maxPoolSize = 50;
    private int writeThreads = 75;
    private int maxReadPoolSize = 50;
    private int readThreads = 75;
    private int maxWriteTasks = 5_000;
    private int maxReadTasks = 5_000;
    private int writeTaskTimeout = 120_000;
    private int readTaskTimeout = 120_000;
    private int acquireConnectionTimeout =30_000;
    private boolean cachePrepStmts = true;
    private int prepStmtCacheSize = 500;
    private int prepStmtCacheSqlLimit = 2048;
    private boolean useServerPrepStmts = true;
    private int defaultQueryTimeoutInSeconds = 20;
    private String storageEngine = "InnoDB";
    private boolean memSQL;
    private int leakDetectionThreshold = 0;

    private boolean dev;
    private boolean prod;
    private boolean test;
    private boolean evolutionEnabled = true;
    private boolean generateSchema;
    private boolean syncIndexes = false;
    private boolean dropColumns = false;
    private String advisorFile = "advisor.sql";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getReadUrl() {
        return readUrl;
    }

    public void setReadUrl(String readUrl) {
        this.readUrl = readUrl;
    }

    public String getReadUser() {
        return readUser;
    }

    public void setReadUser(String readUser) {
        this.readUser = readUser;
    }

    public String getReadPassword() {
        return readPassword;
    }

    public void setReadPassword(String readPassword) {
        this.readPassword = readPassword;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getWriteThreads() {
        return writeThreads;
    }

    public void setWriteThreads(int writeThreads) {
        this.writeThreads = writeThreads;
    }

    public int getMaxReadPoolSize() {
        return maxReadPoolSize;
    }

    public void setMaxReadPoolSize(int maxReadPoolSize) {
        this.maxReadPoolSize = maxReadPoolSize;
    }

    public int getReadThreads() {
        return readThreads;
    }

    public void setReadThreads(int readThreads) {
        this.readThreads = readThreads;
    }

    public int getMaxWriteTasks() {
        return maxWriteTasks;
    }

    public void setMaxWriteTasks(int maxWriteTasks) {
        this.maxWriteTasks = maxWriteTasks;
    }

    public int getMaxReadTasks() {
        return maxReadTasks;
    }

    public void setMaxReadTasks(int maxReadTasks) {
        this.maxReadTasks = maxReadTasks;
    }

    public int getWriteTaskTimeout() {
        return writeTaskTimeout;
    }

    public void setWriteTaskTimeout(int writeTaskTimeout) {
        this.writeTaskTimeout = writeTaskTimeout;
    }

    public int getReadTaskTimeout() {
        return readTaskTimeout;
    }

    public void setReadTaskTimeout(int readTaskTimeout) {
        this.readTaskTimeout = readTaskTimeout;
    }

    public int getAcquireConnectionTimeout() {
        return acquireConnectionTimeout;
    }

    public void setAcquireConnectionTimeout(int acquireConnectionTimeout) {
        this.acquireConnectionTimeout = acquireConnectionTimeout;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public void setCachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
    }

    public int getPrepStmtCacheSize() {
        return prepStmtCacheSize;
    }

    public void setPrepStmtCacheSize(int prepStmtCacheSize) {
        this.prepStmtCacheSize = prepStmtCacheSize;
    }

    public int getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public void setPrepStmtCacheSqlLimit(int prepStmtCacheSqlLimit) {
        this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
    }

    public boolean isUseServerPrepStmts() {
        return useServerPrepStmts;
    }

    public void setUseServerPrepStmts(boolean useServerPrepStmts) {
        this.useServerPrepStmts = useServerPrepStmts;
    }

    public int getDefaultQueryTimeoutInSeconds() {
        return defaultQueryTimeoutInSeconds;
    }

    public void setDefaultQueryTimeoutInSeconds(int defaultQueryTimeoutInSeconds) {
        this.defaultQueryTimeoutInSeconds = defaultQueryTimeoutInSeconds;
    }

    public String getStorageEngine() {
        return storageEngine;
    }

    public void setStorageEngine(String storageEngine) {
        this.storageEngine = storageEngine;
    }

    public boolean isMemSQL() {
        return memSQL;
    }

    public void setMemSQL(boolean memSQL) {
        this.memSQL = memSQL;
    }

    public int getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public void setLeakDetectionThreshold(int leakDetectionThreshold) {
        this.leakDetectionThreshold = leakDetectionThreshold;
    }

    public boolean isDev() {
        return dev;
    }

    public void setDev(boolean dev) {
        this.dev = dev;
    }

    public boolean isProd() {
        return prod;
    }

    public void setProd(boolean prod) {
        this.prod = prod;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isEvolutionEnabled() {
        return evolutionEnabled;
    }

    public void setEvolutionEnabled(boolean evolutionEnabled) {
        this.evolutionEnabled = evolutionEnabled;
    }

    public boolean isGenerateSchema() {
        return generateSchema;
    }

    public void setGenerateSchema(boolean generateSchema) {
        this.generateSchema = generateSchema;
    }

    public boolean isSyncIndexes() {
        return syncIndexes;
    }

    public void setSyncIndexes(boolean syncIndexes) {
        this.syncIndexes = syncIndexes;
    }

    public boolean isDropColumns() {
        return dropColumns;
    }

    public void setDropColumns(boolean dropColumns) {
        this.dropColumns = dropColumns;
    }

    public String getAdvisorFile() {
        return advisorFile;
    }

    public void setAdvisorFile(String advisorFile) {
        this.advisorFile = advisorFile;
    }
}
