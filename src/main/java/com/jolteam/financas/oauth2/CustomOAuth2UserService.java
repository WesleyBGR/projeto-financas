package com.jolteam.financas.oauth2;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.jolteam.financas.dao.UsuarioDAO;
import com.jolteam.financas.enums.Provedor;
import com.jolteam.financas.model.Foto;
import com.jolteam.financas.model.Usuario;
import com.jolteam.financas.oauth2.dto.FacebookOAuth2UserInfo;
import com.jolteam.financas.oauth2.dto.GithubOAuth2UserInfo;
import com.jolteam.financas.oauth2.dto.GoogleOAuth2UserInfo;
import com.jolteam.financas.oauth2.dto.OAuth2UserInfo;
import com.jolteam.financas.service.UsuarioService;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	@Autowired private UsuarioService usuarioService;
	
	@Autowired private UsuarioDAO usuarios;

	// este é o primeiro método a ser chamado após o usuário entrar no
	// Google/Facebook
	// e ceder as permissões ao sistema
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		System.out.println("loadUser()");

		// obtém os dados do usuário (OAuth2) que acabou de ser recebido na requisição
		// por exemplo: nome, e-mail etc
		OAuth2User oAuth2User = super.loadUser(userRequest);
		Map<String, Object> attributes = super.loadUser(userRequest).getAttributes();

		// OAuth2UserInfo é uma classe abstrata, portanto, esta variável aceita qualquer objeto
		// de classes que extendam (extends) ela, por exemplo: GoogleOAuth2UserInfo
		OAuth2UserInfo userInfo = null;

		// esta variável recebe o nome do provedor auth, por exemplo: 'google'
		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		// se o provedor auth for o Google
		if (registrationId.equalsIgnoreCase(Provedor.GOOGLE.toString())) {
			// a variável userInfo receberá uma nova instância da classe
			// GoogleOAuth2UserInfo
			// (que por sua vez extende OAuth2UserInfo)
			// juntamente com os valores de seus atributos
			userInfo = new GoogleOAuth2UserInfo(attributes);
			// se o provedor auth for o Facebook
		} else if (registrationId.equalsIgnoreCase(Provedor.FACEBOOK.toString())) {
			// a variável userInfo receberá uma nova instância da classe
			// FacebookOAuth2UserInfo
			// (que também extende OAuth2UserInfo)
			// juntamente com os valores de seus atributos
			userInfo = new FacebookOAuth2UserInfo(attributes);
		} else if (registrationId.equalsIgnoreCase(Provedor.GITHUB.toString())) {
			// a variável userInfo receberá uma nova instância da classe
			// GithubOAuth2UserInfo
			// (que também extende OAuth2UserInfo)
			// juntamente com os valores de seus atributos
			userInfo = new GithubOAuth2UserInfo(attributes);
		}

		// chama o método que irá checar se o usuário existe ou não, fazer validações
		// e inserir/atualizar (ou não) o usuário no banco
		this.updateUser(userInfo);

		return oAuth2User;
	}

	private void updateUser(OAuth2UserInfo userInfo) {
		System.out.println("updateUser()");

		// obtém o usuário do banco. se não existir cria um novo
		Optional<Usuario> u = this.usuarios.findByEmail(userInfo.getEmail());
		boolean usuarioExiste = u.isPresent() ? true : false;
		Usuario usuario = usuarioExiste ? u.get() : new Usuario();

		// se o usuário existe e for um usuário cadastrado com uma conta de um provedor OAuth
		// (ou seja, não é uma conta local)
		if (usuarioExiste && !usuario.getProvedor().equals(Provedor.LOCAL)) {
			// se o provedor do usuário do banco for o Google
			if (usuario.getProvedor().equals(Provedor.GOOGLE)) {
				// mas o usuário da requisição (que acabou de fazer login) não for um usuário do
				// Google
				if (!(userInfo instanceof GoogleOAuth2UserInfo)) {
					throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"),
							"provedor_invalido_google");
				}
			// se o provedor do usuário do banco for o Facebook
			} else if (usuario.getProvedor().equals(Provedor.FACEBOOK)) {
				// mas o usuário da requisição (que acabou de fazer login) não for um usuário do
				// Facebook
				if (!(userInfo instanceof FacebookOAuth2UserInfo)) {
					throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"),
							"provedor_invalido_facebook");
				}
			// se o provedor do usuário do banco for o GitHub
			} else if (usuario.getProvedor().equals(Provedor.GITHUB)) {
				// mas o usuário da requisição (que acabou de fazer login) não for um usuário do
				// GitHub
				if (!(userInfo instanceof GithubOAuth2UserInfo)) {
					throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"),
							"provedor_invalido_github");
				}
			}
			// resumindo... por exemplo, se um usuário usou sua conta do Google para se
			// cadastrar
			// e depois tentou logar com sua conta do Facebook que possui o mesmo e-mail da
			// conta do Google
			// não será permitido.
		}

		// se o usuário não tiver provedor (ou seja, é um usuário novo)
		// ou o provedor for o Google/Facebook/GitHub
		if (usuario.getProvedor() == null || !usuario.getProvedor().equals(Provedor.LOCAL)) {
			// os atributos nome, sobrenome e e-mail são definidos/atualizados
			// independentemente de
			// o usuário já ser cadastrado ou não
			usuario.setNome(userInfo.getName());
			usuario.setEmail(userInfo.getEmail());

			Foto fotoNova = new Foto(null, null, null, userInfo.getImageUrl());

			// já os atributos a seguir só são definidos se o usuário ainda não for
			// cadastrado
			if (!usuarioExiste) {
				// provedorId é o ID do usuário no provedor auth.
				// geralmente é um número
				usuario.setProvedorId(userInfo.getId());

				usuario.setRegistroData(LocalDateTime.now());
				// para pegar o IP do usuário precisamos da requisição (request)
				// porém, neste método não há um objeto HttpServletRequest como parâmetro
				usuario.setRegistroIp("indefinido");

				// se a foto já existir apenas atualiza o link, senão cria uma nova
				usuario.setFoto(fotoNova);

				// o usuário já terá sua conta ativada, já que é uma conta do Google/Facebook
				usuario.setAtivado(true);

				usuario.setPermissao((short) 1);
			} else {
				if (usuario.getFoto() != null) {
					Foto fotoExistente = usuario.getFoto();
					fotoExistente.setConteudo(fotoNova.getConteudo());
				}
			}

			// se for um usuário do Google
			if (userInfo instanceof GoogleOAuth2UserInfo) {
				usuario.setSobrenome(((GoogleOAuth2UserInfo) userInfo).getFamilyName());
				usuario.setProvedor(Provedor.GOOGLE);
				// se for um usuário do Facebook
			} else if (userInfo instanceof FacebookOAuth2UserInfo) {
				usuario.setSobrenome(((FacebookOAuth2UserInfo) userInfo).getLastName());
				usuario.setProvedor(Provedor.FACEBOOK);
				// se for um usuário do GitHub
			} else if (userInfo instanceof GithubOAuth2UserInfo) {
				if (Strings.isBlank(((GithubOAuth2UserInfo) userInfo).getFullName())) {
					throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"),
							"nome_vazio_github");
				}
				usuario.setSobrenome(((GithubOAuth2UserInfo) userInfo).getSobrenome());
				usuario.setProvedor(Provedor.GITHUB);
			}
		}

		// adiciona/atualiza o usuário no banco
		this.usuarios.save(usuario);
		
		if (!usuarioExiste) {
			this.usuarioService.adicionarCategorias(usuario);
		}
	}

}
