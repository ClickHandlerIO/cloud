package io.clickhandler.sql;

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
    private String readUrl = "jdbc:h2:mem:move;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE;";
    private String readUser = "root";
    private String readPassword = "passme";
    private int maxPoolSize = 2;
    private int maxReadPoolSize = 4;
    private boolean cachePrepStmts = true;
    private int prepStmtCacheSize = 500;
    private int prepStmtCacheSqlLimit = 2048;
    private boolean useServerPrepStmts = true;
    private int defaultQueryTimeoutInSeconds = 4;
    private String storageEngine = "InnoDB";
    private boolean memSQL;

    private boolean dev;
    private boolean prod;
    private boolean test;
    private boolean generateSchema;

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

    public int getMaxReadPoolSize() {
        return maxReadPoolSize;
    }

    public void setMaxReadPoolSize(int maxReadPoolSize) {
        this.maxReadPoolSize = maxReadPoolSize;
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

    public boolean isGenerateSchema() {
        return generateSchema;
    }

    public void setGenerateSchema(boolean generateSchema) {
        this.generateSchema = generateSchema;
    }
}
