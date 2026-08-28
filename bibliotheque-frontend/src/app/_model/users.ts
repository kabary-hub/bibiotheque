export class Users {
    userId: number;
    username: string;
    name: string;
    password: string;
    role: any;
}

/** Corps envoye au backend pour creer ou modifier un compte. */
export class UserRequest {
    username: string;
    name: string;
    password?: string;
    roleIds: number[];
}
