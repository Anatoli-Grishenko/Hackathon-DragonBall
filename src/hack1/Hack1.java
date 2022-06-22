package hack1;

import appboot.JADEBoot;
import appboot.LARVABoot;

public class Hack1 {

    public static void main(String[] args) {
        LARVABoot boot = new LARVABoot();
        boot.Boot("localhost", 1099);
        boot.launchAgent("Krilin", toSongoanda.class);
        boot.WaitToShutDown();
    }
    
}
