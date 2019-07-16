package client.player;

import client.Client;
import java.util.List;
import packet.creators.PacketCreator;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import server.SpeedQuizFactory;
import server.SpeedQuizFactory.QuizEntry;
import tools.Randomizer;

/**
 * @author AuroX
 */

public class SpeedQuiz {

    private final static int INITIAL_QUESTION = 50; 
    private final static int TIME = 15; 
    private final int npc;
    private final byte type;
    private int question;
    private int points;
    private String answer;

    public SpeedQuiz(final Client c, final int npc) {
        this.question = INITIAL_QUESTION;
        this.points = 0;
        this.answer = null;
        this.npc = npc;
        this.type = (byte) Randomizer.nextInt(3); 
        getNewQuestion(c, question);
    }

    public final void nextRound(final Client c, final String answerGiven) {
        if (answerGiven.equals("__GIVEUP__")) {
            giveUp(c);
            return;
        }
        CheckAnswer(answerGiven);
        if (question == 1) { 
            reward(c);
            return;
        }
        question--;
        getNewQuestion(c, question);
    }

    private void getNewQuestion(final Client c, final int questionNo) {
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        if (cm.getNpc() != npc) {
            return;
        }

        final List<QuizEntry> entries = SpeedQuizFactory.getInstance().getQuizDataType(questionNo, type);
        final QuizEntry random = entries.get(Randomizer.nextInt(entries.size()));

        this.answer = random.getAnswer();
        
        c.getSession().write(PacketCreator.GetSpeedQuiz(npc, random.getType(), random.getObjectId(), points, questionNo, TIME));
        cm.setLastMsg((byte) 6);
    }

    private void CheckAnswer(final String answerGiven) {
        if (answerGiven == null || answerGiven.equals("")) {
            return;
        } else if (answer.equalsIgnoreCase(answerGiven)) {
            points++;
        }
    }

    private void giveUp(final Client c) {
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        if (points > 0) {
            c.getPlayer().gainMeso(100 * points, true, true, true); // todo change reward? give up = lesser rewards
        }
        cm.sendNext("Ahhh...Its sad that you're giving up the quiz although you managed to answer " + points + " questions. Here's some mesos as a token of appreciation from me.");
        cm.dispose();
        c.getPlayer().setSpeedQuiz(null);
    }

    private void reward(final Client c) {
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        if (points == 50) {
            if (cm.canHold(1302000, 1)) {
                cm.sendNext("Amazing~ You solved every question. But, I'm sorry to tell you...there hasn't been a single trace of Master M. But since you worked so hard, I'll give you this.");
                c.getPlayer().gainMeso(100000000, true, true, true);
            } else {
                cm.sendNext("I'm really sorry that you do not have enough space to keep the reward...Oh well..Check your inventory next time and try again.");
            }
        } else if (points == 0) {
            cm.sendNext("Wow...You didn't obtained a point at all. Therefore, I can't give you any rewards.");
        } else {
            cm.sendNext("Well done, you've obtained " + points + " points out of " + INITIAL_QUESTION + " points. Here's some reward for you.");
            c.getPlayer().gainMeso(100 * points, true, true, true);
        }
        cm.dispose();
        c.getPlayer().setSpeedQuiz(null);
    }
} 
