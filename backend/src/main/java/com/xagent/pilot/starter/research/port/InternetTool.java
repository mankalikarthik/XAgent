package com.xagent.pilot.starter.research.port;

import com.xagent.pilot.starter.research.domain.FetchedPage;

import java.net.URI;

public interface InternetTool {

    FetchedPage fetch(URI uri);

}