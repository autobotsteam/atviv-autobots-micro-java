package com.autobots.automanager.hateos;

import java.util.List;

public interface Hateoas<T> {
	public void addLink(List<T> lista);
	public void addLink(T objeto);
}
