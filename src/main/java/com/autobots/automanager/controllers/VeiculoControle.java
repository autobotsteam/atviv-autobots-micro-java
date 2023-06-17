package com.autobots.automanager.controllers;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autobots.automanager.entitades.Usuario;
import com.autobots.automanager.entitades.Veiculo;
import com.autobots.automanager.entitades.Venda;
import com.autobots.automanager.hateos.VeiculoHateos;
import com.autobots.automanager.servicos.UsuarioServico;
import com.autobots.automanager.servicos.VeiculoServico;
import com.autobots.automanager.servicos.VendaServico;
import com.autobots.automanager.utils.UsuariosSelecionador;
import com.autobots.automanager.utils.VeiculoSelecionador;

@RestController
@RequestMapping("/veiculo")
public class VeiculoControle {
	
	@Autowired
	private VeiculoServico veiculoServico;
	
	@Autowired
	private VeiculoSelecionador selecionador;
	
	@Autowired
	private UsuariosSelecionador usuarioSelecionador;
	
	@Autowired
	private VeiculoHateos hateos;
	
	@Autowired
	private UsuarioServico servicoUsuario;

	@Autowired
	private VendaServico servicoVenda;
	  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")	
	@GetMapping("/veiculos")
	public ResponseEntity<List<Veiculo>> pegarTodos(){
		List<Veiculo> todos = veiculoServico.pegarTodos();
		HttpStatus status = HttpStatus.CONFLICT;
		if(todos.isEmpty()) {
			status = HttpStatus.NOT_FOUND;
			return new ResponseEntity<List<Veiculo>>(status);
		}else {
			hateos.addLink(todos);
			status = HttpStatus.FOUND;
			ResponseEntity<List<Veiculo>> resposta = new ResponseEntity<List<Veiculo>>(todos, status);
			return resposta;
		}
	}
	  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
	@GetMapping("/veiculos/{id}")
	public ResponseEntity<Veiculo> pegarVeiculoEspecifico(@PathVariable Long id){
		List<Veiculo> todasEmpresas = veiculoServico.pegarTodos();
		Veiculo select = selecionador.selecionar(todasEmpresas, id);
		if(select == null) {
			return new ResponseEntity<Veiculo>(HttpStatus.NOT_FOUND);
		}else {
			hateos.addLink(select);
			return new ResponseEntity<Veiculo>(select, HttpStatus.FOUND);
		}
	}
	  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")	
	@PostMapping("/cadastro/{idUsuario}")
	public ResponseEntity<?> cadastroVeiculo(@PathVariable Long idUsuario, @RequestBody Veiculo body){
		List<Usuario> todos = servicoUsuario.pegarTodos();
		Usuario select = usuarioSelecionador.selecionar(todos, idUsuario);
		if(select != null) {
			select.getVeiculos().add(body);
			body.setProprietario(select);
			veiculoServico.adicionar(body);
			return new ResponseEntity<>("Veiculo cadastrado no usuario: " + select.getNome(), HttpStatus.ACCEPTED);
		}else {
			return new ResponseEntity<>("Usuario não encontrado", HttpStatus.NOT_FOUND);
		}
	}
	  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")	
	@PutMapping("/atualizar/{id}")
	public ResponseEntity<?> atualizarVeiculo(@PathVariable Long id, @RequestBody Veiculo atualizador){
		HttpStatus status = HttpStatus.BAD_REQUEST;
		List<Veiculo> usuarios = veiculoServico.pegarTodos();
		Veiculo usuario = selecionador.selecionar(usuarios, id);
		if (usuario != null) {
			atualizador.setId(id);
			veiculoServico.update(atualizador);
			status = HttpStatus.OK;
			return new ResponseEntity<>("Atualizado",status);
		}else {
			status = HttpStatus.NOT_FOUND;
			return new ResponseEntity<>("Não encontrado",status);			
		}
	}
	
	  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")	
	@DeleteMapping("/deletar/{id}")
	public ResponseEntity<?> deletarVeiculo(@PathVariable Long id){
		List<Veiculo> veiculos = veiculoServico.pegarTodos();
		Veiculo veiculo = selecionador.selecionar(veiculos, id);
		if(veiculo != null) {
			veiculo.setProprietario(null);
			List<Usuario> usuarios = servicoUsuario.pegarTodos();
			List<Venda> vendas = servicoVenda.pegarTodos();
			for(Usuario userVeiculos : usuarios) {
				Set<Veiculo> pertencemAoUser = userVeiculos.getVeiculos();
				for(Veiculo veiculosUsuario : pertencemAoUser) {
					if(veiculosUsuario.getId() == id) {
						userVeiculos.getVeiculos().clear();
						pertencemAoUser.remove(veiculo);
						userVeiculos.getVeiculos().addAll(pertencemAoUser);
					}
				}
			}
			for(Venda vendasVeiculo : vendas) {
				if(vendasVeiculo.getVeiculo().getId().equals(veiculo.getId())) {
					veiculoServico.deletarVendas(vendasVeiculo.getId(), id);
					vendasVeiculo.setVeiculo(null);
				}
			}
			veiculoServico.deletar(id);
			return new ResponseEntity<>("Deletado",HttpStatus.ACCEPTED);
		}else {
			return new ResponseEntity<>("Não encontrado",HttpStatus.NOT_FOUND);			
		}
	}

}