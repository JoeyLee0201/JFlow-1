/**
 * This class derives from https://github.com/reprogrammer/keshmesh/ and is licensed under Illinois Open Source License.
 */
package edu.illinois.jflow.wala.pointeranalysis;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

/**
 * Modified from ReceiverStringContext.java, originally from Keshmesh. Authored by Mohsen Vakilian
 * and Stas Negara. Modified by Nicholas Chen.
 * 
 */
public class ReceiverStringContext implements Context {
	private final ReceiverString receiverString;

	public ReceiverStringContext(ReceiverString receiverString) {
		if (receiverString == null) {
			throw new IllegalArgumentException("null receiverString");
		}
		this.receiverString= receiverString;
	}

	public InstanceKey getReceiver() {
		return receiverString.getReceiver();
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof ReceiverStringContext) && ((ReceiverStringContext)o).receiverString.equals(receiverString);
	}

	@Override
	public int hashCode() {
		return receiverString.hashCode();
	}

	@Override
	public String toString() {
		return "KOBJSENCONT: " + receiverString.toString();
	}

	@Override
	public ContextItem get(ContextKey name) {
		if (JFlowCustomContextSelector.RECEIVER_STRING.equals(name)) {
			return receiverString;
		} else {
			return null;
		}
	}
}
