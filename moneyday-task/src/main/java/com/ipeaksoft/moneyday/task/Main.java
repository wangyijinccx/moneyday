package com.ipeaksoft.moneyday.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Hello world!
 *
 */
public class Main {

	public static volatile boolean running = true;

    public static void main( String[] args )
    {
        final Logger logger = LoggerFactory.getLogger(Main.class);
    	final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/application.xml"});
    	context.start();
        logger.info("Main Thread start success...");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                	context.stop();
                	context.close();
                    logger.info("Main Thread stopped!");
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
                synchronized (Main.class) {
                    running = false;
                    Main.class.notify();
                }
            }
        });


		synchronized (Main.class) {
		    while (running) {
		        try {
		            Main.class.wait();
		        } catch (Throwable e) {
		        }
		    }
		}
    }
}
