package org.daisy.pipeline.tts;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import net.sf.saxon.s9api.XdmNode;

public interface TTSService {

	class SynthesisException extends Exception {
		public SynthesisException(String message, Throwable cause) {
			super(message, cause);
		}

		public SynthesisException(String message) {
			super(message);
		}
	}

	public static class RawAudioBuffer {
		public byte[] output;
		public int offsetInOutput;
	};

	public static class Voice {
		public Voice(String vendor, String name) {
			if (vendor == null)
				this.vendor = "";
			else
				this.vendor = vendor.toLowerCase();
			if (name == null)
				this.name = "";
			else
				this.name = name.toLowerCase();
		}

		public int hashCode() {
			return vendor.hashCode() ^ name.hashCode();
		}

		public boolean equals(Object other) {
			if (other == null)
				return false;
			Voice v2 = (Voice) other;
			return vendor.equals(v2.vendor) && name.equals(v2.name);
		}

		public String toString() {
			return "{vendor:" + (!vendor.isEmpty() ? vendor : "%unknown%") + ", name:"
			        + (!name.isEmpty() ? name : "%unkown%") + "}";
		}

		public String vendor;
		public String name;
	}

	/**
	 * This method will be called once by the TTS Registry before running the
	 * first synthesis step. Nothing should be done in the TTS Service
	 * constructors in order to save resources if the server is never used for
	 * TTS purpose.
	 */
	void initialize() throws SynthesisException;

	/**
	 * Allocate new resources (such as TCP connections) unique for each thread.
	 * All the allocation calls are made in a single thread before any sentence
	 * is processed.
	 * 
	 * @return the resources. It can be null
	 * @throws SynthesisException
	 */
	Object allocateThreadResources() throws SynthesisException;

	/**
	 * All the release calls are made in a single thread after all the sentences
	 * have been processed.
	 * 
	 * @param resources is the object returned by allocateThreadResource()
	 */
	void releaseThreadResources(Object resources) throws SynthesisException;

	/**
	 * Must be thread safe because there is only one Synthesizer instantiated.
	 * 
	 * @param ssml is the SSML to synthesize. You may need to convert it to the
	 *            format understandable for the TTS (e.g. SAPI). The SSML code
	 *            must include the <mark> and the <break/> at the end.
	 * @param voice is the voice the synthesizer must use. It is guaranteed to
	 *            be one returned by getAvailableVoices()
	 * @param output is the resulting raw audio data. Ideally the address of the
	 *            buffer is left unchanged, but a new buffer can be allocated
	 *            when the audio data do not fit in the one provided. The new
	 *            buffer must contain the previous data as well.
	 * @param threadResources is the object returned by
	 *            allocateThreadResource().
	 * @param marks are the returned pairs (markName, offsetInOutput)
	 *            corresponding to the ssml:marks of @param ssml. The order must
	 *            be kept. The provided list is always empty. The offsetInOutput
	 *            are relative to the new data inserted (they start at 0 no
	 *            matter what audioBuffer already contains).
	 */
	void synthesize(XdmNode ssml, Voice voice, RawAudioBuffer audioBuffer,
	        Object threadResources, List<Map.Entry<String, Integer>> marks)
	        throws SynthesisException;

	/**
	 * @return the audio format (sample rate etc...) of the data produced by
	 *         synthesize(). The synthesizer is assumed to use the same audio
	 *         format every time.
	 */
	AudioFormat getAudioOutputFormat();

	/**
	 * @return the same name as in the CSS voice-family property. If several TTS
	 *         services share the same name, then the one with the highest
	 *         priority will be chosen.
	 */
	public String getName();

	/**
	 * @return the version or type (binary, in-memory) of the TTS service. Used
	 *         only for printing information.
	 */
	public String getVersion();

	/**
	 * Called from a single thread
	 */
	public void beforeAllocatingResources() throws SynthesisException;

	/**
	 * Called from a single thread
	 */
	public void afterAllocatingResources() throws SynthesisException;

	/**
	 * Called from a single thread
	 */
	public void beforeReleasingResources() throws SynthesisException;

	/**
	 * Called from a single thread
	 */
	public void afterReleasingResources() throws SynthesisException;

	/**
	 * Called from a single thread
	 */
	public int getOverallPriority();

	/**
	 * Called from a single thread
	 */
	public Collection<Voice> getAvailableVoices() throws SynthesisException;
}
