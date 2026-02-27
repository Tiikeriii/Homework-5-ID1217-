import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.*;


public class DistributedProcess implements RemoteProtocol {
    private static final boolean DEBUG = Boolean.getBoolean("debug");
    private static Random rand = new Random();

    private int size = 10000;
    private Integer[] localArray;
    private List<Integer> localCommonArray;

    // constructor
    public DistributedProcess(String processID, int size) {
        this.size = size;
        this.localArray = generateArray(size);
        this.localCommonArray = new ArrayList<>();
    }

    private Integer[] generateArray(int size) {
        Set<Integer> set = new HashSet<Integer>();
        while (set.size() < size) {
            set.add(rand.nextInt(size*3));
        }
        Integer[] array = set.toArray(new Integer[0]);
        array[0] = 69;
        if (DEBUG) {
            System.out.print("[");
            for (int i = 0; i < array.length; i++) {
                System.out.print(" " + array[i]);
            }
            System.out.println("]");
        }
        return array;
    }

    public boolean checkValue (Integer value) throws RemoteException {
        for (int i = 0; i < localArray.length; i++) {
            if (localArray[i].equals(value)) {
                return true;
            }
        }
        return false;
    }

    public void updateCommonArray (Integer value) throws RemoteException {
        localCommonArray.add(value);
    }

    private void findCommonValues(RemoteProtocol G, RemoteProtocol H) throws RemoteException {
        int index = 0;
        Integer value;
        while (index < size) {
            value = localArray[index];
            if (G.checkValue(value) && H.checkValue(value)) {
                localCommonArray.add(value);
                G.updateCommonArray(value);
                H.updateCommonArray(value);
                System.out.print(value + " ");
            }
            index++;
        }
    }

    public void process(String processID) throws Exception {
        UnicastRemoteObject.exportObject(this, 0);
        Registry registry = LocateRegistry.getRegistry(1099);
        registry.rebind(processID, this);
        
        if (processID.equals("F")) {
            RemoteProtocol G = (RemoteProtocol) registry.lookup("G");
            RemoteProtocol H = (RemoteProtocol) registry.lookup("H");
            findCommonValues(G, H);
        }
        
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || (!args[0].equals("F") && !args[0].equals("G") && !args[0].equals("H"))) {
            System.out.println("Usage: processID size, processID can only be F, G or H");
            return;
        }
        int size = 10000;
        if (args.length > 1) {
            size = Integer.parseInt(args[1]);
        }
        try {
            LocateRegistry.createRegistry(1099);
            System.out.println("RMI registry started on port 1099");
        } catch (RemoteException e) {
            System.out.println("RMI registry already running.");
        }
        new DistributedProcess(args[0], size).process(args[0]);
    }
}