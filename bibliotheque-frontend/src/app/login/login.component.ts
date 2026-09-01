import { Component } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  constructor(private userService: UsersService,
    private userAuthSerivce: UserAuthService,
    private router: Router
  ) { }

  envoiEnCours = false;
  erreur: string | null = null;

  login(loginForm: NgForm) {
    this.erreur = null;
    this.envoiEnCours = true;
    this.userService.login(loginForm.value).subscribe(
      (response: any) => {
        this.envoiEnCours = false;
        this.userAuthSerivce.setRoles(response.user.role);
        this.userAuthSerivce.setToken(response.jwtToken);
        this.userAuthSerivce.setUserId(response.user.userId);
        this.userAuthSerivce.setName(response.user.name);
        const role = response.user.role[0].roleName;
        this.router.navigate([role === 'Admin' ? '/books' : '/my-books']);
      },
      () => {
        this.envoiEnCours = false;
        this.erreur = 'Identifiants incorrects. Vérifiez votre nom d\'utilisateur et votre mot de passe.';
      }
    );
  }

}