package dev.liftoff.platform.proxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.liftoff.platform.common.exception.LiftoffException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaddyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${liftoff.caddy.admin-url}")
    private String caddyAdminUrl;

    /**
     * Registers a route in Caddy for the given subdomain, mapping it to the output directory.
     * Caddy reads the output directory path from its perspective (e.g. /srv/deployments/...)
     */
    public void registerSite(String subdomain, String outputDir) {
        String host = subdomain + ".localhost"; // For local dev. In prod, this would be subdomain.liftoff.dev
        
        log.info("Registering site {} in Caddy to serve from {}", host, outputDir);

        try {
            // Caddy API route format
            // We build the JSON manually to match Caddy's highly nested structure
            ObjectNode route = objectMapper.createObjectNode();
            
            // Matchers: match the host
            ArrayNode match = route.putArray("match");
            ObjectNode hostMatch = match.addObject();
            hostMatch.putArray("host").add(host);

            // Handlers: file_server with root
            ArrayNode handle = route.putArray("handle");
            
            // 1. Root variable handler
            ObjectNode rootHandler = handle.addObject();
            rootHandler.put("handler", "vars");
            rootHandler.put("root", outputDir);
            
            // 2. File server handler
            ObjectNode fileServerHandler = handle.addObject();
            fileServerHandler.put("handler", "file_server");
            fileServerHandler.put("hide", objectMapper.createArrayNode().add(".git*"));

            // To add a route dynamically without overwriting the whole config:
            // POST /config/apps/http/servers/srv0/routes
            // Note: srv0 must exist in Caddy's config. We assume a base config or Caddy creates it if we POST to the right path.
            // A simpler way for MVP is to append to the routes array.
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(route.toString(), headers);
            
            String targetUrl = caddyAdminUrl + "/config/apps/http/servers/srv0/routes";
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, request, String.class);
                log.info("Caddy route registration successful: {}", response.getStatusCode());
            } catch (Exception apiEx) {
                // If srv0 doesn't exist, we might need to initialize the base config first
                log.warn("Failed to add route to srv0, attempting to initialize Caddy config... Error: {}", apiEx.getMessage());
                initializeBaseConfigAndRetry(route);
            }
            
        } catch (Exception e) {
            log.error("Failed to register site in Caddy", e);
            throw new LiftoffException("Failed to configure reverse proxy: " + e.getMessage());
        }
    }

    private void initializeBaseConfigAndRetry(ObjectNode route) throws Exception {
        ObjectNode rootConfig = objectMapper.createObjectNode();
        
        ObjectNode apps = rootConfig.putObject("apps");
        ObjectNode http = apps.putObject("http");
        ObjectNode servers = http.putObject("servers");
        ObjectNode srv0 = servers.putObject("srv0");
        
        srv0.putArray("listen").add(":80").add(":443");
        srv0.putArray("routes").add(route);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(rootConfig.toString(), headers);
        
        ResponseEntity<String> response = restTemplate.exchange(caddyAdminUrl + "/config/", HttpMethod.POST, request, String.class);
        log.info("Initialized Caddy config with route. Status: {}", response.getStatusCode());
    }
}
