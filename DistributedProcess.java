import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.*;


public class DistributedProcess implements RemoteProtocol {
    private static final boolean DEBUG = Boolean.getBoolean("debug");       // debug flag for printing the original arrays
    private static Random rand = new Random();      // shared random object for array generation

    private int size;       // default size
    private Integer[] localArray;   // local array for each process
    private List<Integer> localCommonArray;     // local array for common values for each process

    // constructor
    public DistributedProcess(String processID, int size) {
        this.size = size;
        this.localArray = generateArray(size);
        this.localCommonArray = new ArrayList<>();
    }

    // generates an array without duplicates from 0 to size*3
    private Integer[] generateArray(int size) {
        Set<Integer> set = new HashSet<Integer>();
        while (set.size() < size) {
            set.add(rand.nextInt(size*3));
        }
        Integer[] array = set.toArray(new Integer[0]);
        array[0] = 69;
        // debug print for troubleshooting
        if (DEBUG) {
            System.out.print("[");
            for (int i = 0; i < array.length; i++) {
                System.out.print(" " + array[i]);
            }
            System.out.println(" ]");
        }
        return array;
    }

    // returns true if a value is in a local array
    public boolean checkValue (Integer value) throws RemoteException {
        for (int i = 0; i < localArray.length; i++) {
            if (localArray[i].equals(value)) {
                return true;
            }
        }
        return false;
    }

    // updates the localCommonArray
    public void updateCommonArray (Integer value) throws RemoteException {
        localCommonArray.add(value);
    }

    // process F runs and finds all common variables in all three arrays
    private void findCommonValues(RemoteProtocol G, RemoteProtocol H) throws RemoteException {
        int index = 0;
        Integer value;
        // while loop that iterates through process F's array and compares with G and H
        while (index < size) {
            value = localArray[index];
            if (G.checkValue(value) && H.checkValue(value)) {
                localCommonArray.add(value);
                G.updateCommonArray(value);
                H.updateCommonArray(value);
            }
            index++;
        }
    }

    // prints each local common array and then terminates the process
    public void printTerminate(String processID) throws RemoteException {
        System.out.print("Common values printed by process " + processID + " -> [");
        for (int i = 0; i < localCommonArray.size(); i++) {
            System.out.print(" " + localCommonArray.get(i));
        }
        System.out.println(" ]");
        System.out.println("Process terminated");
        UnicastRemoteObject.unexportObject(this, true);
    }

    // start method for the processes
    public void process(String processID) throws Exception {
        UnicastRemoteObject.exportObject(this, 0);      // makes the object available for remote access
        Registry registry = LocateRegistry.getRegistry(1099);   // locates the local RMI registry
        registry.rebind(processID, this);       // places this object in the registry with name "processID"

        RemoteProtocol G = null;
        RemoteProtocol H = null;
        
        // only process F executes this
        if (processID.equals("F")) {
            // find the other processes or wait if they have not yet started
            while (true) {
                try {
                    G = (RemoteProtocol) registry.lookup("G");
                    H = (RemoteProtocol) registry.lookup("H");
                    break;
                } catch (Exception e) {
                    Thread.sleep(100);
                }
            }
            findCommonValues(G, H);     // initiate common search
            // each process prints their common arrays and then terminates
            G.printTerminate("G");
            H.printTerminate("H");
            printTerminate("F");
        }

    }
    public static void main(String[] args) throws Exception {
        // Bad Request
        if (args.length < 1 || (!args[0].equals("F") && !args[0].equals("G") && !args[0].equals("H"))) {
            System.out.println("Usage: processID size, processID can only be F, G or H");
            return;
        }
        int size = 10000;   // default size
        // set size if there is an input argument for size
        if (args.length > 1) {
            size = Integer.parseInt(args[1]);
        }
        // start RMI registry on port 1099
        try {
            LocateRegistry.createRegistry(1099);
            System.out.println("RMI registry started on port 1099");
        } catch (RemoteException e) {
            System.out.println("RMI registry already running.");
        }
        // construct a new DistributedProcess object and start the initialization
        new DistributedProcess(args[0], size).process(args[0]);
    }
}