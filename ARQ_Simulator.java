import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ARQ_Simulator {
    static final int MAX_SEQ = 63;       // sequence numbers 0–63
    static final int FRAME_SIZE = 1200;  // bytes per frame
    static final int WIN_GBN = 63;       // Go-Back-N window = 2^k - 1
    static final int WIN_SR  = 32;       // Selective Repeat window = 2^(k-1)

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        // 1. Get algorithm input
        System.out.print("Which flow control algorithm? (1: Go-Back-N ARQ or 2: Selective-Repeat ARQ): ");
        int algo = scan.nextInt();
        scan.nextLine();
        if (algo != 1 && algo != 2) {
            System.out.println("Error: please enter 1 or 2 only.");
            return;
        }

        // 2. Get file name
        System.out.print("What is the input file name? ");
        String fileName = scan.nextLine().trim();
        byte[] fileData;
        try {
            fileData = Files.readAllBytes(Paths.get(fileName));
        } 
        catch (IOException e) {
            System.out.println("Error: cannot open file.");
            return;
        }

        // 3. Split into frames
        int totalFrames = (int) Math.ceil((double) fileData.length / FRAME_SIZE);
        System.out.println("\nThe total number of frames to be transmitted is " + totalFrames + ".");

        // 4. Randomly select 5% lost frames
        int lostCount = (int) Math.ceil(totalFrames * 0.05); // # of loss frames = 5% from total frames
        Set<Integer> lost = pickLostFrames(totalFrames, lostCount); // use the method to randomly pick the lost frame
        List<Integer> lostFrames = new ArrayList<>(lost); // an arraylist to keep the lost frames
        Collections.sort(lostFrames); // sort the selected lost frames

        // print all the lost frames
        System.out.print("The loss frames are ");
        for (int i = 0; i < lostFrames.size(); i++) {
            System.out.print("Frame " + lostFrames.get(i));
            if (i < lostFrames.size() - 1) System.out.print(", ");
        }
        System.out.println(".\n");

        // 5. Run algorithm
        if (algo == 1) {
            System.out.println("Go-Back-N ARQ (Window Size = " + WIN_GBN + "; Sequence Number 0 to 63)");
            GoBackN(totalFrames, lostFrames);
        } else {
            System.out.println("Selective-Repeat ARQ (Window Size = " + WIN_SR + "; Sequence Number 0 to 63)");
            SelectiveRepeat(totalFrames, lostFrames);
        }
    }


    // Pick unique lost frame numbers
    static Set<Integer> pickLostFrames(int total, int count) {
        Random r = new Random();
        Set<Integer> lost = new HashSet<>();
        while (lost.size() < count && total > 0) {
            lost.add(r.nextInt(total)); // add a random lost frame from total frames into the set
        }
        return lost;
    }

    // --- Go-Back-N ---
    static void GoBackN(int total, List<Integer> lostFrames) {
        Set<Integer> lost = new HashSet<>(lostFrames);
        int duplicatedAck = 0;
        int expectedAck = -1;
        Integer lostFrame = null;

        for (int i = 0; i < total; i++) {
            int seq = i % 64; // 0-63
            if (lost.contains(i)) {
                System.out.println("Frame " + i + ": Seq No. " + seq + " (Loss)  -");
                lostFrame = i;
                expectedAck = (seq + 1) % 64;
                duplicatedAck = 0;
            } else if (lostFrame != null) {
                // receiver keeps sending duplicate ACKs
                System.out.println("Frame " + i + ": Seq No. " + seq + "   ACK " + expectedAck);
                duplicatedAck++;
                if (duplicatedAck == 3) {
                    // retransmit lost and following frames
                    for (int j = lostFrame; j <= i; j++) {
                        int sq = j % 64; // sq = sequence
                        System.out.println("Frame " + j + ": Seq No. " + sq + " (Retransmit) ACK " + ((sq + 1) % 64));
                    }
                    lostFrame = null; // clear lost frame
                }
            } else {
                // normal transmission
                System.out.println("Frame " + i + ": Seq No. " + seq + "   ACK " + ((seq + 1) % 64));
            }
        }
    }

    // --- Selective Repeat ---
    static void SelectiveRepeat(int total, List<Integer> lostFrames) {
        Set<Integer> lost = new HashSet<>(lostFrames);
        Integer lostFrame = null;

        for (int i = 0; i < total; i++) {
            int seq = i % 64; // 0-63
            if (lost.contains(i)) {
                System.out.println("Frame " + i + ": Seq No. " + seq + " (Loss)  -");
                lostFrame = i;
            } else if (lostFrame != null) {
                int lostSeq = lostFrame % 64;
                System.out.println("Frame " + i + ": Seq No. " + seq + "   NACK " + lostSeq);
                System.out.println("Frame " + lostFrame + ": Seq No. " + lostSeq + " (Retransmit) ACK " + ((lostSeq + 1) % 64));
                lostFrame = null;
            } else {
                System.out.println("Frame " + i + ": Seq No. " + seq + "   ACK " + ((seq + 1) % 64));
            }
        }

        // If last frame lost, retransmit at end
        if (lostFrame != null) {
            int lostSeq = lostFrame % 64;
            System.out.println("Frame " + lostFrame + ": Seq No. " + lostSeq + " (Retransmit) ACK " + ((lostSeq + 1) % 64));
        }
    }
}
