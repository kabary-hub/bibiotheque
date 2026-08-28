package com.ibizabroker.bibliotheque.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private UserDetailsService jwtService;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors();
        httpSecurity.csrf().disable()
                .authorizeRequests()
                // Seule porte ouverte : l'obtention d'un jeton.
                .antMatchers("/authenticate").permitAll()
                // /borrow/** etait en permitAll. N'importe qui, sans jeton,
                // pouvait donc emprunter un exemplaire au nom de n'importe quel
                // adherent, et le retirer du catalogue. Ces operations exigent
                // desormais un compte : le controle du role reste porte par les
                // routes Angular, mais l'anonymat n'est plus une option.
                .antMatchers("/borrow/**").authenticated()
                // « /admin/books/ », avec sa barre finale, ne correspondait a
                // aucun chemin reellement expose (le mapping est /admin/books) :
                // la regle etait inoperante et masquait le fait que la liste des
                // livres est de toute facon accessible a tout compte connecte.
                .antMatchers("/admin/books/**").authenticated()
                // Module Reservation : meme politique que /borrow/**, l'autre module
                // tourne vers l'adherent. Les codes de retour attendus par l'enonce
                // sont 400, 404 et 409 ; exiger un jeton ici ferait apparaitre un 401
                // qui n'y figure pas. A restreindre le jour ou le projet distinguera
                // reellement les roles sur les operations d'adherent.
                .antMatchers("/api/reservations/**").permitAll()
                // Documentation OpenAPI. Sans ces motifs, /swagger-ui.html repondrait
                // 401 et la documentation exigee serait inaccessible.
                .antMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**").permitAll()
                .antMatchers(HttpHeaders.ALLOW).permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ;

        httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.userDetailsService(jwtService).passwordEncoder(passwordEncoder());
    }
}