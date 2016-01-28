package edu.cornell.cs.nlp.spf.explat.resources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

/**
 * Creator to read a serialized object from a file.
 *
 * @author Yoav Artzi
 *
 */
public class SerializedObjectCreator implements IResourceObjectCreator<Object> {

	public static final ILogger LOG = LoggerFactory
			.create(SerializedObjectCreator.class);

	private final String type;

	public SerializedObjectCreator() {
		this("serialized");
	}

	public SerializedObjectCreator(String type) {
		this.type = type;
	}

	@Override
	public Object create(Parameters params, IResourceRepository repo) {
		try (final ObjectInput input = new ObjectInputStream(
				new BufferedInputStream(
						new FileInputStream(params.getAsFile("file"))))) {
			final Object object = input.readObject();
			LOG.info("Read %s from %s", object.getClass(),
					params.getAsFile("file"));
			return object;
		} catch (final IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public ResourceUsage usage() {
		return ResourceUsage.builder("serialized", Object.class)
				.setDescription("Read a serialized object from a file")
				.addParam("file", File.class, "Input file").build();
	}

}
