package com.jasonhhouse.Gaps;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles REST and WebSocket calls for Gaps.
 */
@Controller
public class GapsControllerImpl {

    private final Logger logger = LoggerFactory.getLogger(GapsControllerImpl.class);

    private final GapsSearch gapsSearch;
    @Autowired
    private final SimpMessagingTemplate template;

    @Autowired
    GapsControllerImpl(GapsSearch gapsSearch, SimpMessagingTemplate template) {
        this.gapsSearch = gapsSearch;
        this.template = template;
    }

    /**
     * Searches Plex for the "Movie" libraries it can find
     *
     * @param address Host name of the machine to connect to Plex on
     * @param port    Port Plex runs on
     * @param token   User specific Plex token
     * @return List of PlexLibraries found
     */
    @RequestMapping(value = "getPlexLibraries", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<PlexLibrary>> getPlexLibraries(@RequestParam("address") String address,
                                                             @RequestParam("port") int port,
                                                             @RequestParam("token") String token) {
        logger.info("getPlexLibraries()");
        Set<PlexLibrary> plexLibraries = gapsSearch.getPlexLibraries(address, port, token);
        return new ResponseEntity<>(plexLibraries, HttpStatus.OK);
    }

    /**
     * Forces a stop to searching. Used commonly if navigated away from page.
     *
     * @return success of search canceled
     */
    @RequestMapping(value = "cancelSearch", method = RequestMethod.PUT)
    public ResponseEntity<String> cancelSearch() {
        logger.info("cancelSearch()");
        gapsSearch.cancelSearch();
        return new ResponseEntity<>("Canceled Search", HttpStatus.OK);
    }

    /**
     * Main REST call to start Gaps searching for missing movies
     *
     * @param gaps Needs the gaps object to get started with Plex information and The Movie DB key
     * @return All missing movies with their release year and collections associated to them
     */
    @RequestMapping(value = "submit", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public Future<ResponseEntity<Set<Movie>>> submit(@RequestBody Gaps gaps) {
        logger.info("submit()");

        //Error checking
        if (StringUtils.isEmpty(gaps.getMovieDbApiKey())) {
            String reason = "Missing Movie DB Api Key. This field is required for Gaps.";
            logger.error(reason);

            Exception e = new IllegalArgumentException();
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason, e);
        }

        if (BooleanUtils.isNotTrue(gaps.getSearchFromPlex()) && BooleanUtils.isNotTrue(gaps.getSearchFromFolder())) {
            String reason = "Must search from Plex and/or folders. One or both of these fields is required for Gaps.";
            logger.error(reason);

            Exception e = new IllegalArgumentException();
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason, e);
        }

        if (BooleanUtils.isNotFalse(gaps.getSearchFromPlex())) {
            if (CollectionUtils.isEmpty(gaps.getMovieUrls())) {
                String reason = "Missing Plex movie collection urls. This field is required to search from Plex.";
                logger.error(reason);

                Exception e = new IllegalArgumentException();
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason, e);
            } else {
                for (String url : gaps.getMovieUrls()) {
                    if (StringUtils.isEmpty(url)) {
                        String reason = "Found empty Plex movie collection url. This field is required to search from Plex.";
                        logger.error(reason);

                        Exception e = new IllegalArgumentException();
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason, e);
                    }
                }
            }
        }

        //Fill in default values if missing
        if (gaps.getWriteTimeout() == null) {
            logger.info("Missing write timeout. Setting default to 180 seconds.");
            gaps.setWriteTimeout(180);
        }

        if (gaps.getConnectTimeout() == null) {
            logger.info("Missing connect timeout. Setting default to 180 seconds.");
            gaps.setConnectTimeout(180);
        }

        if (gaps.getReadTimeout() == null) {
            logger.info("Missing read timeout. Setting default to 180 seconds.");
            gaps.setReadTimeout(180);
        }

        return gapsSearch.run(gaps);
    }

    /**
     * WebSocket connection that when Gaps is searching returns the updated collection results for more real time
     * visualization of Gaps
     */
    @Scheduled(fixedDelay = 1000)
    public void currentSearchResults() {
        if (gapsSearch.isSearching() && CollectionUtils.isNotEmpty(gapsSearch.getRecommendedMovies())) {
            logger.info("currentSearchResults()");

            List<Movie> newMovies = gapsSearch.getRecommendedMovies();

            if (CollectionUtils.isNotEmpty(newMovies)) {
                SearchResults searchResults = new SearchResults(gapsSearch.getSearchedMovieCount(), gapsSearch.getTotalMovieCount(), newMovies);
                template.convertAndSend("/topic/currentSearchResults", searchResults);
            }
        }
    }

}
