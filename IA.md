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