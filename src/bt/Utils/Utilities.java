package bt.Utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import bt.Exceptions.UnknownBittorrentException;
import bt.Model.Bittorrent;
import bt.View.ClientGUI;
import bt.View.UserInterface;

/**
 * A pure utilities class filled of static methods for specific tasks.
 * 
 * @author Isaac Yochelson, Robert Schomburg and Fernando Geraci
 *
 */
public class Utilities {
	
	public static final int MAX_PIECE_LENGTH = 16384;
	public static final int MAX_TIMEOUT = 120000;
	
	/**
	 * Returns a byte stream from the given file.
	 * @param file
	 * @return byte[] File Bytes
	 */
	public static byte[] getBytesFromFile(File file) {
		byte[] bytesArray = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			bytesArray = new byte[(int)raf.length()];
			raf.read(bytesArray);
			raf.close();
		} catch (Exception e) {
			System.out.println("Random Access File failed.");
		}
		return bytesArray;
	}
	
	/**
	 * Writes the log to a file called log.txt in the root directory.
	 * @param log
	 */
	public static void saveLogToFile(String log) {
		File logFile = new File("log.txt");
		if(logFile.exists()) {
			try {
				logFile.delete();
			} catch (Exception e) { }
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(logFile);
			DataOutputStream dos = new DataOutputStream(fos);
			StringTokenizer st = new StringTokenizer(log, "\n");
			while(st.hasMoreTokens()) {
				dos.write(st.nextToken().getBytes());
				dos.write(System.getProperties().getProperty("line.separator").getBytes());
			}
		} catch (Exception e) { }
	}
	
	/**
	 * Returns a String representation of a ByteBuffer.
	 * @param bb ByteBuffer to be converted to a string
	 * @return String message
	 */
	public static String getStringFromByteBuffer(ByteBuffer bb) {
		StringBuilder message = new StringBuilder();
		int bytes;
		while(true) {
			try {
				bytes = bb.get();
				// format the product of two bytes and a bitwise AND with 0xFF
				message.append("\\x"+String.format("%02x", bytes&0xff));
			} catch (Exception e) {
				break;
			}
		}
		return message.toString();
	}
	
	/**
	 * It will encode the info_hash to a URL parameter recursively.
	 * @param infoHash The sha hash of the torrent file.
	 * @return String encoded info_hash URL
	 */
	public static String encodeInfoHashToURL(String infoHash) {
		String encodedURL = "";
		if(infoHash.length() == 0) return infoHash;
		char ch = infoHash.charAt(0);
		if(ch == 'x') 
			encodedURL += "%"+encodeInfoHashToURL(infoHash.substring(1));
		else if(ch == '\\')
			encodedURL += encodeInfoHashToURL(infoHash.substring(1));
		else
			encodedURL += ch+encodeInfoHashToURL(infoHash.substring(1));
		return encodedURL;
	}
	
	/**
	 * Generates a random 20 character string as a peerID.
	 * @return String peer ID
	 */
	public static String generateID() {
		StringBuilder generatedID = new StringBuilder();
		char nextChar;
		for(int i = 0 ; i < 20; ++i) {
			// create a random character between 65 - 90 ASCII
			nextChar = (char)(65 + (int)(Math.random()*25));
			generatedID.append(nextChar);
		}
		ClientGUI.getInstance().publishEvent("Random ID is (in bytes): "+generatedID.toString());
		
		return generatedID.toString();
	}
	
	/**
	 * Returns a peer list from the ByteBuffer return.
	 * @param map A collection of peers.
	 * @return decoded peer list
	 */
	 public static String[] decodeCompressedPeers(Map map) {
	        ByteBuffer peers = (ByteBuffer)map.get(ByteBuffer.wrap("peers".getBytes()));
	        ArrayList<String> peerURLs = new ArrayList<String>();
	        try {
	            while (true) {
	                String ip = String.format("%d.%d.%d.%d",
	                    peers.get() & 0xff,
	                    peers.get() & 0xff,
	                    peers.get() & 0xff,
	                    peers.get() & 0xff);
	                int firstByte = (0x000000FF & ((int)peers.get()));
	                int secondByte = (0x000000FF & ((int)peers.get()));
	                int port  = (firstByte << 8 | secondByte);
	                // int port = peers.get() * 256 + peers.get();
	                peerURLs.add(ip + ":" + port);
	            }
	        } catch (Exception e) {
	        }
	        return peerURLs.toArray(new String[peerURLs.size()]);
	  }
	 
	 /**
	  * Returns the update interval requested by tracker
	  * @param map: the interval
	  * @return the integer value
	  */
	 public static int decodeInterval(Map map) {
		 int interval = -1;
		 try {
			 interval = (int)map.get(ByteBuffer.wrap("interval".getBytes()));
		 } catch (Exception e) {}
		 return interval;		 
	 }
	 
	 /**
	  * Returns the updated min_interval requested by tracker
	  * @param map: the min_interval key
	  * @return the integer value of min_interval
	  */
	 public static int decodeMinInterval(Map map) {
		 int min_interval = -1;
		 try {
			 min_interval = (int)map.get(ByteBuffer.wrap("min_interval".getBytes()));
		 } catch (Exception e) {
			 //System.err.println("Tracker min_interval = -1, because not received.");
		 }
		 return min_interval;		 
	 }
	 
	 /**
	  * Terminates the client.
	  */
	 public static void callClose() {
		 try {
			 
			 Bittorrent.getInstance().stopServer();
			 Bittorrent.getInstance().disposePeers();
			 Bittorrent.getInstance().saveHeap();
			 System.exit(0);
		 } catch (Exception e) { /* this should never happen */	 } 
	 }
	 
	 /**
	  * Splits the IPv4 and port from address.
	  * @param address The A string representation of the ip address
	  * @return String IPv4 address
	  */
	 public static String getIPFromString(String address) {
		 String ipAddress = null;
		 int separator = address.indexOf(':');
		 if(separator != -1) {
			 ipAddress = address.substring(0, separator);
		 }
		 return ipAddress;
	 }
	 
	 /**
	  * Splits the port section of a String IPv4:port String.
	  * @param address
	  * @return integer value of port string
	  */
	 public static int getPortFromString(String address) {
		 int port = -1;
		 int separator = address.indexOf(':');
		 if(separator != -1) {
			 port = Integer.parseInt(address.substring(separator+1));
		 }
		 return port;
	 }
	 
	 /**
	  * Concatenates two byte arrays
	  * @param a byte array to be prepended
	  * @param b byte array to be appended
	  * @return single concatenated byte array
	  */
	 public byte[] byteConcat(byte[] a, byte[] b) {
		   byte[] C= new byte[a.length + b.length];
		   System.arraycopy(a, 0, C, 0, a.length);
		   System.arraycopy(b, 0, C, a.length, b.length);
		   return C;
	 }
	  
	 /**
	  * Returns a byte[] from the info_hash ByteBuffer for simplicity.
	  * @param bb ByteBuffer containing the hash code
	  * @return byte[] info_hash 20 bytes
	  */
	 public static byte[] getHashBytes(ByteBuffer bb) {
		 bb.position(0);
		 byte[] array = new byte[20];
		 bb.get(array);
		 return array;
	 }
	 
	 /**
	  * It tests two arrays of bytes for equality.
	  * @param a byte array to be checked for equality
	  * @param b byte array to be checked for equality
	  * @return boolean True if match, false otherwise.
	  */
	 public static boolean matchBytes(byte[] a, byte[] b) {
		 if(a.length != b.length) return false;
		 else {
			 for(int i = 0; i < a.length; ++i) {
				 if(a[i] != b[i]) return false;
			 }
			 return true;
		 }
	 }
	 
	 /**
	  * Extracts the info hash from the handshake repsonse.
	  * @param response the response sent by the peer to our handshake
	  * @return info_hash
	  */
	 public static byte[] getInfoHashFromHandShakeResponse(byte[] response) {
		 byte[] info_hash = new byte[20];
		 int offset = 28;
		 for(int i = 0; i < 20; ++i) {
			 info_hash[i] = response[offset];
			 ++offset;
		 }
		 return info_hash;
	 }
	 
	 /**
	  * Returns the index of the next piece to be requested.
	  * @param completed
	  * @return index of a piece which is needed.
	  */
	 public static int getNeededPiece(boolean[] completed) {

		 for(int i = 0; i < completed.length; ++i) {
			 if(!completed[i]) return i;
		 }
		 return -1; // no more pieces are needed.
	 }
	 
	 /**
	  * Converts a bitfield into a string.
	  * @param boolean[] bits
	  * @return String bitfield string representation.
	  */
	 public static String bitFieldToString(boolean[] bits) {
		 String bitfield = "";
		 for(int i = 0; i < bits.length; ++i) {
			 if(bits[i]) {
				 bitfield += "1";
			 } else {
				 bitfield += "0";
			 }
		 }
		 return bitfield;
		 
	 }
	 
	 /**
	  * Saves the client state 
	  * @param downloaded
	  * @param uploaded
	  * @param left
	  * @param fileHeap
	  * @param temp
	  * @throws IOException
	  */
	 public static void saveState (int downloaded, int uploaded, int left, byte[][] fileHeap, File temp) throws IOException { 
		 FileOutputStream tempOut = new FileOutputStream(temp);  
		 ByteBuffer intBuffer = ByteBuffer.allocate(12);  
		 intBuffer.putInt(downloaded).putInt(uploaded).putInt(left);  
		 byte[] ints = intBuffer.array();  
		 // intBuffer.get(ints);  
		 tempOut.write(ints);  
		 for(int i = 0; i < fileHeap.length; ++i) {  
          tempOut.write(fileHeap[i]);  
		 }
		 tempOut.close();
	 }	  
	 
	 /**
	  * Loads previous client's state if file wasn't completed.
	  * @param intArray
	  * @param fileHeap
	  * @param pieceLength
	  * @param pieces
	  * @param temp
	  * @param completed
	  * @param torrentInfo
	  * @param verificationArray
	  * @throws IOException
	  * @throws NoSuchAlgorithmException
	  * @throws UnknownBittorrentException
	  */
	 public static void loadState(int[] intArray, byte[][] fileHeap, 
			 int pieceLength, 
			 int pieces, 
			 File temp, 
			 boolean[] completed, 
			 TorrentInfo torrentInfo,
			 byte[][] verificationArray) throws IOException, 
	 NoSuchAlgorithmException, UnknownBittorrentException {  
		 	FileInputStream tempIn = new FileInputStream(temp);  
		 	byte[] intByteArray = new byte[12];  
		 	tempIn.read(intByteArray, 0, 12);  
		 	ByteBuffer intBuffer = ByteBuffer.wrap(intByteArray);  
		 	for (int i = 0; i < 3; i++) {  
		 		intArray[i] = intBuffer.getInt();  
		 	} 
		 	byte[] bytes = Utilities.getBytesFromFile(temp);
		 	int offset = 12;
		 	for (int i = 0; i < pieces; ++i) {  
		 		// tempIn.read(fileHeap[i], (i * pieceLength) + 12, pieceLength);
		 		for(int u = 0; u < pieceLength; u++) {
		 			fileHeap[i][u] = bytes[offset];
		 			++offset;
		 		}
		 	}
		 	Utilities.verifyPiecesSHA(fileHeap, completed, torrentInfo, verificationArray);
		 	tempIn.close();
	 } 
	 
	 /**
	  * Verify the pieces loaded from .tmp file to complete the client's state bitfield.
	  * @param fileHeap
	  * @param completed
	  * @throws UnknownBittorrentException
	  */
	 private static void verifyPiecesSHA(byte[][] fileHeap, boolean[] completed, TorrentInfo ti, byte[][] verificationArray) throws UnknownBittorrentException {
		 MessageDigest sha = null;
		 try {
			 sha = MessageDigest.getInstance("SHA-1");
		 } catch (NoSuchAlgorithmException e) {
			 // decide what to do
		 }
		 int pieceLength = ti.piece_length; 
		 for( int i = 0; i < fileHeap.length; ++i) { // for each piece
			 // verify
			 byte[] toDigest = null;
				if (i < fileHeap.length - 1) {
					toDigest = new byte[pieceLength];
					synchronized(fileHeap) {
						// load full-sized piece to be hashed
						for(int u = 0; u < toDigest.length; ++u) {
							toDigest[u] = fileHeap[i][u];
					}
				}
			} else {
				toDigest = new byte[ti.file_length - ((fileHeap.length-1)*pieceLength)];
				synchronized(fileHeap) {
					// load possibly partial-sized piece to be hashed
					for(int s = 0; s < toDigest.length; ++s) {
						toDigest[s] = fileHeap[i][s];
					}
				}
			}
			byte[] test = sha.digest(toDigest);
			if (sameArray(verificationArray[i], test)) {
				completed[i] = true;
			} else {
				completed[i] = false;
			}
		 } 
	 }
	 
	 /**
	 * Checks if two byte arrays contain the same values at all positions.
	 * @param first An operand to be tested.
	 * @param second An operand to be tested.
	 * @return returns true if first and second are equal in length and every byte they contain is
	 * of equal value, and false otherwise.
	 */
	public static boolean sameArray (byte[] first, byte[] second) {
		if (first.length != second.length){
			return false;
		} else {
			for (int i = 0; i < first.length; ++i) {
				if (first[i] != second[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Loads the downloaded torrent file into the client's file heap.
	 * @param fileName
	 * @throws Exception
	 */
	public static void initializeFileHeap(String fileName) throws Exception {
		Bittorrent bt = Bittorrent.getInstance();
		int pieceLength = bt.getPieceLength();
		File torrentFile = new File(fileName);
		byte[] fileBytes = Utilities.getBytesFromFile(torrentFile);
		byte[][] fileHeap = bt.getFileHeap();
		int offset = 0;
		mainLoop : for(int i = 0; i < fileHeap.length; ++i) {
			for(int u = 0; u < pieceLength; u++) {
				if(offset < bt.getFileLength()) {
					fileHeap[i][u] = fileBytes[offset];
					offset++;
				} else {
					break mainLoop;
				}
			}
		}
		System.out.println("");
	}
}
