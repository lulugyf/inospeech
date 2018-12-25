package gyf.test;

public class Log {
    public static void d(String tag, String msg){
        System.out.println("d["+tag+"] "+msg);
    }
    public static void d(String tag, String msg, Throwable e){
        System.out.println("d["+tag+"] "+msg);
        e.printStackTrace(System.out);
    }

    public static void e(String tag, String msg){
        System.out.println("e["+tag+"] "+msg);
    }
    public static void e(String tag, String msg, Throwable e){
        System.out.println("e["+tag+"] "+msg);
        e.printStackTrace(System.out);
    }
}
