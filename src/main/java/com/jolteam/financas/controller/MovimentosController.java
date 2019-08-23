package com.jolteam.financas.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jolteam.financas.enums.TipoLog;
import com.jolteam.financas.enums.TipoTransacao;
import com.jolteam.financas.model.Log;
import com.jolteam.financas.model.Transacao;
import com.jolteam.financas.model.Usuario;
import com.jolteam.financas.model.dto.RelatorioForm;
import com.jolteam.financas.service.CategoriaService;
import com.jolteam.financas.service.LogService;
import com.jolteam.financas.service.MovimentosService;
import com.jolteam.financas.util.Util;

@Controller
public class MovimentosController {
	
	@Autowired private CategoriaService categorias;
	@Autowired private MovimentosService movimentosService;
	@Autowired private LogService logService;
	
	@GetMapping("/movimentos")
	public ModelAndView viewMovimentos(@RequestParam(required = false) String erro, 
			@RequestParam(required = false) Integer mesErro, @RequestParam(required = false) Integer anoErro,
			HttpSession session, RedirectAttributes ra) 
	{
		ModelAndView mv = new ModelAndView("usuario/movimentos");
		
		// relacionado a listagem dos dados da página
		Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
		BigDecimal totalReceitas = this.movimentosService.totalReceitaAcumuladaDe(usuario);
		BigDecimal totalDespesas = this.movimentosService.totalDespesaAcumuladaDe(usuario);
		
		mv.addObject("transacao", new Transacao());
		mv.addObject("listatransacao", this.movimentosService.listarTodasPorUsuario(usuario));
		mv.addObject("categoria", this.categorias.listarTodasPorUsuario(usuario));
		
		mv.addObject("totalReceitas", totalReceitas);
		mv.addObject("totalDespesas", totalDespesas);
		mv.addObject("saldoAtual", totalReceitas.subtract(totalDespesas));
		
		// relacionado ao relatório
		int mesAtual = Util.obterValorMesAtual();
		int anoAtual = Util.obterAnoAtual();
		
		RelatorioForm relatorioForm = new RelatorioForm(mesAtual, anoAtual);
		
		if (erro != null && erro.equals("sem_dados")) {
			mv.addObject("msgErro", "Nenhuma movimentação encontrada para o mês/ano informados.");
			relatorioForm = new RelatorioForm(mesErro, anoErro);
		}
		
		mv.addObject("relatorioForm", relatorioForm);
		mv.addObject("anos", Util.obterAnosDeCadastradoDoUsuario(usuario));

		return mv;
	}
	
	
	@GetMapping("/movimentos/excluir")
	@Transactional
	public String excluirTransacao(@RequestParam Integer id, @RequestParam(name = "l", required = false) String local, 
			HttpSession session, HttpServletRequest request, RedirectAttributes ra) 
	{
		try {
			Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
			Optional<Transacao> transacao = this.movimentosService.buscarPorIdEUsuario(id, usuario);
			if (transacao.isPresent()) {
				this.movimentosService.deletarPorIdEUsuario(id, usuario);
				ra.addFlashAttribute("msgSucessoExcluir", "Transação excluída com sucesso!");
				
				if (transacao.get().getTipo().equals(TipoTransacao.RECEITA)) {
					this.logService.save(new Log(usuario, TipoLog.EXCLUSAO_RECEITA, LocalDateTime.now(), 
							Util.getUserIp(request)));
				} else if (transacao.get().getTipo().equals(TipoTransacao.DESPESA)) {
					this.logService.save(new Log(usuario, TipoLog.EXCLUSAO_DESPESA, LocalDateTime.now(), Util.getUserIp(request)));
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		if (local != null && local.equals("RH")) {
			return "redirect:/receitas/historico";
		} else if (local != null && local.equals("DH")) {
			return "redirect:/despesas/historico";
		}
		
		return "redirect:/movimentos";
	}

}
