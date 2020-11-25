package com.sercomm.openfire.plugin.cache;

import java.io.Externalizable;

import org.jivesoftware.util.cache.Cacheable;
import org.xmpp.resultsetmanagement.Result;

public interface CacheBase extends Cacheable, Externalizable, Result
{
}
