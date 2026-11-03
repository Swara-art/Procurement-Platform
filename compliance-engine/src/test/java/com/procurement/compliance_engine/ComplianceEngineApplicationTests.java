package com.procurement.compliance_engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.procurement.compliance_engine.entity.*;
import com.procurement.compliance_engine.repository.*;
import com.procurement.compliance_engine.service.BidService;
import com.procurement.compliance_engine.service.TenderService;
import com.procurement.compliance_engine.security.UserPrincipal;
import org.springframework.web.server.ResponseStatusException;
import java.util.TimeZone;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ComplianceEngineApplicationTests {

	static { TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata")); }
	@Autowired MockMvc mvc;
	@Autowired OrganizationRepository organizationRepository;
	@Autowired BidderProfileRepository bidderProfileRepository;
	@Autowired TenderRepository tenderRepository;
	@Autowired BidRepository bidRepository;
	@Autowired BidService bidService;
	@Autowired TenderService tenderService;
	@Autowired ObjectMapper objectMapper;

	@AfterEach void clearSecurityContext(){SecurityContextHolder.clearContext();}

	@Test
	void contextLoads() {
	}

	@Test void protectedApiRejectsAnonymousRequests() throws Exception {
		mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/tenders")).andExpect(status().isUnauthorized());
	}

	@Test void csrfTokenIsAvailableWithoutAuthenticationAndCookieEndpointsStillRequireIt() throws Exception {
		mvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
		// Cookie refresh/logout flows stay CSRF-protected even though they are anonymous at the authorization layer.
		mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/auth/logout").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk());
	}

	@Test @Transactional void publicRegistrationAlwaysCreatesBidderRole() throws Exception {
		String unique=java.util.UUID.randomUUID().toString();
		String body="""
			{"fullName":"Test Bidder","email":"%s@example.test","password":"Strong-Passphrase-7!","organizationName":"Test Org %s","registrationNumber":"REG-%s","role":"SYSTEM_ADMIN"}
			""".formatted(unique,unique,unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.user.role").value("BIDDER"));
	}

	@Test @Transactional void publicOrganizationAndAdminSignupOnlyCreatePendingRequests() throws Exception {
		String unique=UUID.randomUUID().toString();
		String orgBody="{\"fullName\":\"Authority User\",\"email\":\"org-%s@example.test\",\"password\":\"Strong-Passphrase-7!\",\"actorType\":\"ORGANIZATION\",\"organizationName\":\"Authority %s\",\"registrationNumber\":\"AUTH-%s\"}".formatted(unique,unique,unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(orgBody)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.user").doesNotExist());
		String adminBody="{\"fullName\":\"Admin Requester\",\"email\":\"admin-%s@example.test\",\"password\":\"Strong-Passphrase-7!\",\"actorType\":\"ADMIN\"}".formatted(unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(adminBody)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test @Transactional void duplicateBidderEmailIsRejected() throws Exception {
		String unique=java.util.UUID.randomUUID().toString();
		String body="""
			{"fullName":"Test Bidder","email":"%s@example.test","password":"Strong-Passphrase-7!","organizationName":"Test Org %s","registrationNumber":"REG-%s"}
			""".formatted(unique,unique,unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body)).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body)).andExpect(status().isConflict());
	}

	@Test @Transactional void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
		String unique=java.util.UUID.randomUUID().toString();
		String register="""
			{"fullName":"Test Bidder","email":"%s@example.test","password":"Strong-Passphrase-7!","organizationName":"Login Org %s","registrationNumber":"LOGIN-%s"}
			""".formatted(unique,unique,unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(register)).andExpect(status().isCreated());
		String login="{\"email\":\"%s@example.test\",\"password\":\"Strong-Passphrase-7!\"}".formatted(unique);
		mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(login))
				.andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("BIDDER"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("HttpOnly")));
	}

	@Test @Transactional void bidderCanRegisterLoginRefreshLogoutAndUseProtectedProfile() throws Exception {
		String unique=UUID.randomUUID().toString();
		String register="{\"fullName\":\"Auth Flow User\",\"email\":\"flow-%s@example.test\",\"password\":\"Strong-Passphrase-7!\",\"actorType\":\"BIDDER\",\"organizationName\":\"Flow Org %s\",\"registrationNumber\":\"FLOW-%s\"}".formatted(unique,unique,unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(register)).andExpect(status().isCreated());
		String login="{\"email\":\"flow-%s@example.test\",\"password\":\"Strong-Passphrase-7!\"}".formatted(unique);
		var loginResponse=mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(login)).andExpect(status().isOk()).andReturn().getResponse();
		String accessToken=objectMapper.readTree(loginResponse.getContentAsString()).get("accessToken").asText();
		jakarta.servlet.http.Cookie refreshCookie=loginResponse.getCookie("refresh_token");
		assertNotNull(refreshCookie);
		mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+accessToken)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("BIDDER"));
		var csrfResponse=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
		String csrfToken=objectMapper.readTree(csrfResponse.getContentAsString()).get("token").asText();
		jakarta.servlet.http.Cookie csrfCookie=csrfResponse.getCookie("XSRF-TOKEN");
		assertNotNull(csrfCookie);
		var refreshResponse=mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie,csrfCookie).header("X-XSRF-TOKEN",csrfToken)).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn().getResponse();
		jakarta.servlet.http.Cookie rotatedCookie=refreshResponse.getCookie("refresh_token");
		assertNotNull(rotatedCookie);
		mvc.perform(post("/api/v1/auth/logout").cookie(rotatedCookie,csrfCookie).header("X-XSRF-TOKEN",csrfToken)).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/refresh").cookie(rotatedCookie,csrfCookie).header("X-XSRF-TOKEN",csrfToken)).andExpect(status().isUnauthorized());
	}

	@Test @Transactional void incorrectPasswordIsRejected() throws Exception {
		String unique=java.util.UUID.randomUUID().toString();
		String register="""
			{"fullName":"Test Bidder","email":"%s@example.test","password":"Strong-Passphrase-7!","organizationName":"Login Org %s","registrationNumber":"LOGIN-%s"}
			""".formatted(unique,unique,unique);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(register)).andExpect(status().isCreated());
		String login="{\"email\":\"%s@example.test\",\"password\":\"Wrong-Passphrase-8!\"}".formatted(unique);
		mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(login)).andExpect(status().isUnauthorized());
	}

	@Test @Transactional void bidderCanSeePublicTendersAcrossAuthoritiesButOnlyOwnBids() {
		String suffix=UUID.randomUUID().toString();
		Organization authorityA=organization("Authority A "+suffix,"PROCUREMENT_AUTHORITY",suffix+"-A");
		Organization authorityB=organization("Authority B "+suffix,"PROCUREMENT_AUTHORITY",suffix+"-B");
		Organization bidderA=organization("Bidder A "+suffix,"BIDDER",suffix+"-C");
		Organization bidderB=organization("Bidder B "+suffix,"BIDDER",suffix+"-D");
		BidderProfile profileA=profile(bidderA),profileB=profile(bidderB);
		Tender tenderA=tender(authorityA,"TA-"+suffix,"PUBLISHED");
		Tender tenderB=tender(authorityB,"TB-"+suffix,"PUBLISHED");
		tender(authorityB,"TD-"+suffix,"DRAFT");
		Bid bidA=bid(tenderA,profileA),bidB=bid(tenderA,profileB);
		authenticate("BIDDER",bidderA.getId());
		List<UUID> discoverableTenderIds=tenderService.getAllTenders(null).stream().map(x->x.id()).toList();
		assertTrue(discoverableTenderIds.contains(tenderA.getId()));
		assertTrue(discoverableTenderIds.contains(tenderB.getId()));
		assertFalse(discoverableTenderIds.contains(tenderRepository.findByTenderNumber("TD-"+suffix).orElseThrow().getId()));
		assertEquals(List.of(bidA.getId()),bidService.getVisibleBids().stream().map(x->x.id()).toList());
		assertThrows(ResponseStatusException.class,()->bidService.getBidById(bidB.getId()));
	}

	@Test @Transactional void governmentUsersOnlySeeBidsForTheirOwnAuthority() {
		String suffix=UUID.randomUUID().toString();
		Organization authorityA=organization("Officer Authority A "+suffix,"PROCUREMENT_AUTHORITY",suffix+"-A");
		Organization authorityB=organization("Officer Authority B "+suffix,"PROCUREMENT_AUTHORITY",suffix+"-B");
		Organization bidder=organization("Officer Bidder "+suffix,"BIDDER",suffix+"-C");
		BidderProfile profile=profile(bidder);
		Tender ownTender=tender(authorityA,"OA-"+suffix,"PUBLISHED");
		Tender otherTender=tender(authorityB,"OB-"+suffix,"PUBLISHED");
		Bid ownBid=bid(ownTender,profile),otherBid=bid(otherTender,profile);
		authenticate("PROCUREMENT_OFFICER",authorityA.getId());
		assertEquals(List.of(ownBid.getId()),bidService.getVisibleBids().stream().map(x->x.id()).toList());
		assertThrows(ResponseStatusException.class,()->bidService.getBidById(otherBid.getId()));
	}

	private Organization organization(String name,String type,String registration){Organization o=new Organization();o.setName(name);o.setOrganizationType(type);o.setRegistrationNumber(registration);return organizationRepository.saveAndFlush(o);}
	private BidderProfile profile(Organization organization){BidderProfile p=new BidderProfile();p.setOrganization(organization);return bidderProfileRepository.saveAndFlush(p);}
	private Tender tender(Organization authority,String number,String status){Tender t=new Tender();t.setAuthority(authority);t.setTenderNumber(number);t.setTitle(number);t.setDescription("test");t.setStatus(status);t.setSubmissionDeadline(OffsetDateTime.now().minusDays(1));return tenderRepository.saveAndFlush(t);}
	private Bid bid(Tender tender,BidderProfile profile){Bid b=new Bid();b.setTender(tender);b.setBidder(profile);b.setQuotedAmount(new BigDecimal("1000"));b.setStatus("SUBMITTED");b.setSubmittedAt(OffsetDateTime.now().minusHours(1));return bidRepository.saveAndFlush(b);}
	private void authenticate(String role,UUID organizationId){UserPrincipal user=new UserPrincipal(UUID.randomUUID(),"tenant-test@example.in","",role,organizationId,"ACTIVE");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));}

}
