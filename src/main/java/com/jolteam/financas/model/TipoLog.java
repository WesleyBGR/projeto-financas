package com.jolteam.financas.model;

public enum TipoLog {
	
	LOGIN("Login"), 
	
	CADASTRO_RECEITA("Cadastro de Receita"), 
	CADASTRO_DESPESA("Cadastro de Despesa"), 
	CADASTRO_COFRE("Cadastro de Cofre"), 
	
	CADASTRO_CATEGORIA_RECEITA("Cadastro de Categoria de Receita"), 
	CADASTRO_CATEGORIA_DESPESA("Cadastro de Categoria de Despesa"), 
	CADASTRO_CATEGORIA_COFRE("Cadastro de Categoria de Cofre"), 
	
	EXCLUSAO_RECEITA("Exclusão de Receita"), 
	EXCLUSAO_DESPESA("Exclusão de Despesa"), 
	EXCLUSAO_COFRE("Exclusão de Cofre"), 
	
	EXLUSAO_CATEGORIA_RECEITA("Exclusão de Categoria de Receita"), 
	EXCLUSAO_CATEGORIA_DESPESA("Exclusão de Categoria de Despesa"), 
	EXCLUSAO_CATEGORIA_COFRE("Exclusão de Categoria de Cofre"),
	
	ERRO_CADASTRO_RECEITA("Erro no Cadastro de Receita"),
	ERRO_CADASTRO_DESPESA("Erro no Cadastro de Despesa"),
	ERRO_CADASTRO_COFRE("Erro no Cadastro de Cofre"),
	
	ERRO_CADASTRO_CATEGORIA_RECEITA("Erro no Cadastro de Categoria de Receita"),
	ERRO_CADASTRO_CATEGORIA_DESPESA("Erro no Cadastro de Categoria de Despesa"),
	ERRO_CADASTRO_CATEGORIA_COFRE("Erro no Cadastro de Categoria de Cofre");
	
	public String texto;
	TipoLog(String texto) {
		this.texto = texto;
	}
	
}