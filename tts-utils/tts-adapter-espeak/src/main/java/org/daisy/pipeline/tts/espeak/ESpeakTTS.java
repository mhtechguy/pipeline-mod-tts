package org.daisy.pipeline.tts.espeak;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.sound.sampled.AudioFormat;

import net.sf.saxon.s9api.XdmNode;

import org.daisy.pipeline.tts.TTSService;

public class ESpeakTTS implements TTSService {

	@Override
	public AudioFormat getAudioOutputFormat() {
		return null;
	}

	@Override
	public String getName() {
		return "espeak";
	}

	@Override
	public void synthesize(XdmNode ssml, Voice voice, RawAudioBuffer audioBuffer,
	        Object resource, List<Entry<String, Integer>> marks) throws SynthesisException {
	}

	@Override
	public Object allocateThreadResources() {
		return null;
	}

	@Override
	public void releaseThreadResources(Object resource) {
	}

	@Override
	public String getVersion() {
		return null;
	}

	@Override
	public void beforeAllocatingResources() throws SynthesisException {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterAllocatingResources() throws SynthesisException {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeReleasingResources() throws SynthesisException {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterReleasingResources() throws SynthesisException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getOverallPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<Voice> getAvailableVoices() throws SynthesisException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() throws SynthesisException {
		// TODO Auto-generated method stub

	}

}
