package co.kr.woojjam.concurrency.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.kr.woojjam.concurrency.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchController {

	private final MatchService matchService;

	@PostMapping("/init")
	public void init() {
		matchService.deleteAll();
		matchService.init();
	}

}
