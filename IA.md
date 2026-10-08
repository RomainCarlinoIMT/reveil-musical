# Justfications et choix des outils IA

Le but de ce markdown et d'exposer les prompt IA utiliser et d'expliquer les choix techniques.

## Prompt 1
```
Peut tu créer seulement la base du projet à partir du pdf. Le projet sera coder en Java avec Spring pour pouvoir faire de l'IOC. Obligation de d'ajouter seulement des dépendances libre si impossible, il faudrait que tu t'arrête et me propose d'autre language de programmation ou techno.
```

Pour la première requête de création de la base, j'ai explicitement demander à l'IA de ne choisir que des dépendences libre. Le but étant de faire exprès de mettre en question mes préference et termes de d'architecure. J'ai aussi choisi de seulement créer une base avent d'attaquer le code dans le but de faire exprès que l'IA ne sache pas trop du projet.

## Prompt 2
```
La, prochaine étape est de créer les classes qui implémente TrackProvider. Il faudra faire l'appel aux API et unifier le format de ces API pour qu'il respecte le format de donné de Track.java. Il faudra aussi faire une implementation avec des valeurs en dure dans le code pour repondre à l'exigence du fallback de secours.
```

Ayant les interfaces prête j'ai choisi comme orientation de commencer par les rêquetes d'API externe. J'ai bien préciser de suivre les interfaces existante pour éviter que l'IA ne se dispères trop. Et j'ai choisi de faire en sorte que l'une des implementation soit le service de fallback.

## Prompt 3
```
Maintenant le but est de mettre en place des implémentation pour l'envois des notifications. Le but n'est pas de faire tout de suite la liason avec les recherche déjà mise en place. Il faudra bien créer différente classes mais pas d'envois de messages réel. l'utilisation de log précissant le type d'envois et le message de l'envois est suffisant.
```

Ici j'ai choisit de mettre en place les notifications car c'est un module qui a été assez bien découper donc sont implementation ne devrais pas trop changer pour la suite du tp. Mais il reste toujours extensible pour le future.

## Prompt 3
```
Maintenant le but va être de en place une implementation de la gestion d'un utlisateur. Le but est de proposer une gestion minimaliste des informations d'un utilisateur. L'utilisateur pourra créer un compte (pseudo) qui aboutira a un userID unique (on pourras utiliser une lib de UUID). Attention ici ne pas faire la gestion des préferences, le but est de seulement préparer la basse pour faire le lien avec ce composant pour le future.
```

Ici j'ai l'intension de mettre en place un service de gestion des utilisateurs le plus décolérer des préférences utilisateurs. Tout cela dans le but de dire qui si plus tard la gestion de l'utilisateur venait a être plus complexe alors la logique des préférences serait peut impacter et inversement.

## Prompt 4
```
Maintenant le but va être de mettre en place le stockage des préference, méthode d'envois des notifications, préference de musiques par rapport à la météo, heure d'envois. Et la partie la plus important est de faire une MAP qui va lien un jour de la semaine et un type de méteo à un morceux de musique précis. J'aimerais ajouter une touche personnel aux consignes qui serait de faire en sorte que l'utilisateur puisse enregister plusieurs morceaux par conditions dans le but de créer une diversiter dans les réponse de notre service.
```

Après avoir la base de tout les autres composants, j'ai finalement mis en place le coeur du projet. J'ai choisit de stocker pour chaque utilisateurs, des préferences qui sont vraiment le coeur du projet tout en laissant place a de futures extensions. J'ai aussi ajouter une touche personnaliser ou l'utilisateurt pourrais enregister plusieurs musiques pour une conditions dans le but d'ajouter du dynanamise à l'application.
## Prompt 5
```
Le prochain objectif est de mettre en place, la base de l'API, l'idéal serait une API du type REST. Le but est de faire pour le moment seulement la gestion de la création d'un utlisateur et d'ajout des préférence de musique. Il faudrait idéalment créer un controlleur specifique pour chaque besoin (un pour l'utilisateur et un pour les pref).
```

Ici j'ai demander de mettre en place la partie API de notre servir et indrectement de faire le lien entre toutes les parties précendentes. Il faut noter que l'IA à fait la remarque interresante de separer des préferences utilisateurs la partie purement utilisateur (methode d'envois, heure d'envois) et la partie musique. Cela a permis de repondre à mon besoin de basse de faire deux controlleurs pour éviter d'avoir du single point of failure et donc de repondre à l'exigence de l'énnoncer d'avoir un mode dégradder possible.

## Prompt 6
```
En relisant les consignes il faudrait ajouter dans les préference de l'utilisatteurs la prise en compte de la sources de musique de préference. Cela va permettre de voir si le code est extensible facilement
```

Après relecture des consignes j'ai vu que je ne traitais pas le cas de la sources de préférence. J'ai donc donner comme simple consigne à l'IA d'ajouter ce champ. Ici c'est une erreur qui peut servir dans le sens ou ça me permet de tester si le code est extensible ou non, en réference à la consignes oral "imaginons que l'application peut passer à 200 000 utilisateur dans le future"

Note : Les test unitaires me rassure sur la fiablité des modifications