package com.baeldung.client.web.controller;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;

import com.baeldung.client.web.model.FooModel;

@Controller
public class FooClientController {

	@Value("${resourceserver.api.foo.url:http://localhost:8081/resource-server/api/foos/}")
	private String fooApiUrl;

	@Autowired
	private WebClient webClient;

	@GetMapping("/foos")
	public String getFoos(@RegisteredOAuth2AuthorizedClient("custom") OAuth2AuthorizedClient authorizedClient,
			Model model) {
		List<FooModel> foos = this.webClient.get().uri(fooApiUrl).attributes(oauth2AuthorizedClient(authorizedClient))
				.retrieve().bodyToMono(new ParameterizedTypeReference<List<FooModel>>() {
				}).block();
		model.addAttribute("foos", foos);
		return "foos";
	}

	@GetMapping("/addfoo")
	public String addNewFoo(Model model) {
		model.addAttribute("foo", new FooModel(0L, ""));
		return "addfoo";
	}

	@PostMapping("/foos")
	public String saveFoo(FooModel foo, Model model) {
		try {
			this.webClient.post().uri(fooApiUrl).bodyValue(foo).retrieve().bodyToMono(Void.class).block();
			return "redirect:/foos";
		} catch (final HttpServerErrorException e) {
			model.addAttribute("msg", e.getResponseBodyAsString());
			return "addfoo";
		}
	}

}
