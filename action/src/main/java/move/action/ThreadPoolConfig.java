package move.action;

public class ThreadPoolConfig {
   private int coreSize;
   private int maxSize;

   public ThreadPoolConfig() {
   }

   public int coreSize() {
      return this.coreSize;
   }

   public int maxSize() {
      return this.maxSize;
   }

   public ThreadPoolConfig coreSize(final int coreSize) {
      this.coreSize = coreSize;
      return this;
   }

   public ThreadPoolConfig maxSize(final int maxSize) {
      this.maxSize = maxSize;
      return this;
   }


}
