package com.googlesource.gerrit.plugins.websession.flatfile;

import com.google.gwt.editor.client.Editor.Path;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.websession.flatfile.FlatFileWebSession.Module;

import org.easymock.EasyMockSupport;
import org.junit.Test;

public class FlatFileWebSessionTest extends EasyMockSupport{

  @Test
  public void testGetWebSessionDir() throws Exception {
    Injector injector = Guice.createInjector(new Module());
    injector.getProvider(Path.class);

  }

}
