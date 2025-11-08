import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ARQ_Simulator {
    static final int MAX_SEQ = 63;       // sequence numbers 0–63
    static final int FRAME_SIZE = 1200;  // bytes per frame
    static final int WIN_GBN = 63;       // Go-Back-N window = 2^k - 1
    static final int WIN_SR  = 32;       // Selective Repeat window = 2^(k-1)
    static final long SEED = 217;        // fixed seed for reproducible losses

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        // 1. Get algorithm choice
        System.out.print("Which flow control algorithm? (1: Go-Back-N ARQ or 2: Selective-Repeat ARQ): ");
        int mode = scan.nextInt();
        scan.nextLine(); // consume newline
        if (mode != 1 && mode != 2) {
            System.out.println("Error: please enter 1 or 2 only.");
            return;
        }

        // 2. Get file name
        System.out.print("What is the input file name? ");
        String fileName = scan.nextLine().trim();
        byte[] fileData;
        try {
            fileData = Files.readAllBytes(Paths.get(fileName));
        } catch (IOException e) {
            System.out.println("Error: cannot open file.");
            return;
        }

        // 3. Split into frames
        int totalFrames = (int) Math.ceil((double) fileData.length / FRAME_SIZE);
        System.out.println("\nThe total number of frames to be transmitted is " + totalFrames + ".");

        // 4. Randomly select 5% lost frames
        int lostCount = (int) Math.ceil(totalFrames * 0.05); // # of loss frames = 5% from total frames
        Set<Integer> lost = pickLostFrames(totalFrames, lostCount); // use the method to randomly pick the loss frame
        List<Integer> lostList = new ArrayList<>(lost); // an arraylist to keep the loss frames
        Collections.sort(lostList); // sort the selected loss frames

        // print all the loss frames
        System.out.print("The loss frames are ");
        for (int i = 0; i < lostList.size(); i++) {
            System.out.print("Frame " + lostList.get(i));
            if (i < lostList.size() - 1) System.out.print(", ");
        }
        System.out.println(".\n");

        // 5. Run algorithm
        if (mode == 1) {
            System.out.println("Go-Back-N ARQ (Window Size = " + WIN_GBN + "; Sequence Number 0 to 63)");
            simulateGBN(totalFrames, lostList);
        } else {
            System.out.println("Selective-Repeat ARQ (Window Size = " + WIN_SR + "; Sequence Number 0 to 63)");
            simulateSR(totalFrames, lostList);
        }
    }

    // === Helper Methods ===

    // Pick unique lost frame numbers
    static Set<Integer> pickLostFrames(int total, int count) {
        Random r = new Random(SEED);
        Set<Integer> lost = new HashSet<>();
        while (lost.size() < count && total > 0) {
            lost.add(r.nextInt(total));
        }
        return lost;
    }

    // --- Go-Back-N ---
    static void simulateGBN(int total, List<Integer> lostList) {
        Set<Integer> lost = new HashSet<>(lostList);
        int dupAck = 0;
        int expectedAck = -1;
        Integer lostFrame = null;

        for (int i = 0; i < total; i++) {
            int seq = i % 64;
            if (lost.contains(i)) {
                System.out.println("Frame " + i + ": Seq No. " + seq + " (Loss)  -");
                lostFrame = i;
                expectedAck = (seq + 1) % 64;
                dupAck = 0;
            } else if (lostFrame != null) {
                // receiver keeps sending duplicate ACKs
                System.out.println("Frame " + i + ": Seq No. " + seq + "   ACK " + expectedAck);
                dupAck++;
                if (dupAck == 3) {
                    // fast retransmit lost and following frames
                    for (int j = lostFrame; j <= i; j++) {
                        int s = j % 64;
                        System.out.println("Frame " + j + ": Seq No. " + s + " (Retransmit) ACK " + ((s + 1) % 64));
                    }
                    lostFrame = null;
                }
            } else {
                // normal transmission
                System.out.println("Frame " + i + ": Seq No. " + seq + "   ACK " + ((seq + 1) % 64));
            }
        }
    }

    // --- Selective Repeat ---
    static void simulateSR(int total, List<Integer> lostList) {
        Set<Integer> lost = new HashSet<>(lostList);
        Integer pendingLoss = null;

        for (int i = 0; i < total; i++) {
            int seq = i % 64;
            if (lost.contains(i)) {
                System.out.println("Frame " + i + ": Seq No. " + seq + " (Loss)  -");
                pendingLoss = i;
            } else if (pendingLoss != null) {
                int lostSeq = pendingLoss % 64;
                System.out.println("Frame " + i + ": Seq No. " + seq + "   NACK " + lostSeq);
                System.out.println("Frame " + pendingLoss + ": Seq No. " + lostSeq + " (Retransmit) ACK " + ((lostSeq + 1) % 64));
                pendingLoss = null;
            } else {
                System.out.println("Frame " + i + ": Seq No. " + seq + "   ACK " + ((seq + 1) % 64));
            }
        }

        // If last frame lost, retransmit at end
        if (pendingLoss != null) {
            int lostSeq = pendingLoss % 64;
            System.out.println("Frame " + pendingLoss + ": Seq No. " + lostSeq + " (Retransmit) ACK " + ((lostSeq + 1) % 64));
        }
    }
}
