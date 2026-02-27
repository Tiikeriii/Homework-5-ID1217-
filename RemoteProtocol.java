import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteProtocol extends Remote {
    boolean checkValue(Integer value) throws RemoteException;
    void updateCommonArray (Integer value) throws RemoteException;
    void printTerminate(String processID) throws RemoteException;
}
