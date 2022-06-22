/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hack1;

import agents.LARVAFirstAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class toSongoanda extends LARVAFirstAgent {

    enum Status {
        START, CHECKIN, CHECKOUT, OPENPROBLEM, CLOSEPROBLEM, SOLVEPROBLEM, EXIT
    }
    Status myStatus;
    String service = "PMANAGER", problem = "AnswerToSongoanda",
            problemManager = "", content, sessionKey, sessionManager;
    ACLMessage open, session;
    String[] contentTokens;
    
    @Override
    public void setup() {
        this.enableDeepLARVAMonitoring();
        super.setup();
        logger.onTabular();
        myStatus=Status.START;
    }
   
    @Override
    public void Execute() {
         Info("Status: " + myStatus.name());
        switch (myStatus) {
            case START:
                myStatus = Status.CHECKIN;
                break;
            case CHECKIN:
                myStatus = MyCheckin();
                break;
            case OPENPROBLEM:
                myStatus = MyOpenProblem();
                break;
            case SOLVEPROBLEM:
                myStatus = MySolveProblem();
                break;
            case CLOSEPROBLEM:
                myStatus = MyCloseProblem();
                break;
            case CHECKOUT:
                myStatus = MyCheckout();
                break;
            case EXIT:
            default:
                doExit();
                break;
        }
    }
    
    @Override
   public void takeDown() {
       Info("Taking down...");
       this.saveSequenceDiagram("./"+getLocalName()+".seqd");
       super.takeDown();
   }    
    
    public Status MyCheckin() {
        Info("Loading passport and checking-in to LARVA");
        //this.loadMyPassport("config/ANATOLI_GRISHENKO.passport");
        if (!doLARVACheckin()) {
            Error("Unable to checkin");
            return Status.EXIT;
        }
        return Status.OPENPROBLEM;
    }

    public Status MyCheckout() {
        this.doLARVACheckout();
        return Status.EXIT;
    }

    public Status MyOpenProblem() {
        
        if (this.DFGetAllProvidersOf(service).isEmpty()) {
            Error("Service PMANAGER is down");
            return Status.CHECKOUT;
        }
        problemManager = this.DFGetAllProvidersOf(service).get(0);
        Info("Found problem manager " + problemManager);
        this.outbox = new ACLMessage();
        outbox.setSender(getAID());
        outbox.addReceiver(new AID(problemManager, AID.ISLOCALNAME));
        outbox.setContent("Request open " + problem);
        this.LARVAsend(outbox);
        Info("Request opening problem " + problem + " to " + problemManager);
        open = LARVAblockingReceive();
        Info(problemManager + " says: " + open.getContent());
        content = open.getContent();
        contentTokens = content.split(" ");
        if (contentTokens[0].toUpperCase().equals("AGREE")) {
            sessionKey = contentTokens[4];
            session = LARVAblockingReceive();
            sessionManager = session.getSender().getLocalName();
            Info(sessionManager + " says: " + session.getContent());
            return Status.SOLVEPROBLEM;
        } else {
            Error(content);
            return Status.CHECKOUT;
        }
    }

    public Status MySolveProblem() {
        Info("Say Hello to " + sessionManager);
        outbox = session.createReply();
        outbox.setContent("Hello");
        LARVAsend(outbox);        
        Info("Waiting answer from " + sessionManager);
        inbox=LARVAblockingReceive();
        content = inbox.getContent();
        contentTokens = content.split(" ");
        if (!contentTokens[0].toUpperCase().equals("FAILURE")) {
            outbox = inbox.createReply();
            String answer = new StringBuilder(content).reverse().toString();
            outbox.setContent(answer);
            this.LARVAsend(outbox);
            Info("Sending " + answer + " to Songoanda");
            return Status.CLOSEPROBLEM;
        } else {
            Error(content);
            return Status.CHECKOUT;
        }
    }

    public Status MyCloseProblem() {
        outbox = open.createReply();
        outbox.setContent("Cancel session " + sessionKey);
        Info("Closing problem "+problem+" session " + sessionKey);
        this.LARVAsend(outbox);
        inbox = LARVAblockingReceive();
        Info(problemManager + " says: " + inbox.getContent());
        return Status.CHECKOUT;
    }

}
