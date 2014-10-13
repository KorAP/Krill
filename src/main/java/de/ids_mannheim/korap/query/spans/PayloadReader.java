package de.ids_mannheim.korap.query.spans;

import java.nio.ByteBuffer;

import org.apache.lucene.util.BytesRef;

public class PayloadReader {

	/**	Get the offset bytes from the payload.
	 * */
	public static byte[] readOffset(BytesRef payload){
		byte[] b = new byte[8];
		System.arraycopy(payload.bytes, payload.offset, b, 0, 8);
		return b;
	}
	
	/**	Get the end position bytes from the payload and cast it to int. 
	 * */
	public static int readInteger(BytesRef payload, int start) {
		byte[] b = new byte[4];
		System.arraycopy(payload.bytes, payload.offset + start, b, 0, 4);
		return ByteBuffer.wrap(b).getInt();		
	}
	
	/**	Get the elementRef bytes from the payload and cast it into short.
	 * */
	public static short readShort(BytesRef payload, int start) {
    	byte[] b = new byte[2];
    	System.arraycopy(payload.bytes, payload.offset + start, b, 0, 2);
    	return ByteBuffer.wrap(b).getShort();
	}
}
