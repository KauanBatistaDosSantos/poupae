# 📱 Poupaê – App de Controle Financeiro

Poupaê é um aplicativo mobile de controle financeiro pessoal voltado especialmente para jovens brasileiros. Com uma interface moderna e intuitiva, permite acompanhar entradas, saídas, metas financeiras e orçamentos mensais, promovendo educação financeira de forma simples.

---

## 🔧 Funcionalidades Principais

- Registro de transações (ganhos e despesas).
- Marcação de transações como recorrentes.
- Visualização de extrato com filtros por período.
- Criação e acompanhamento de metas financeiras.
- Controle de orçamento mensal com receitas e despesas fixas.
- Saldo real (todas as transações) e saldo recorrente (baseado no orçamento).
- Gráficos comparativos por categoria e mês.
- Interface com navegação inferior (BottomNavigationView).
- Autenticação com Google via Firebase.

---

## ⚙️ Instruções de Configuração

### Pré-requisitos

- Android Studio instalado
- Conta no Firebase configurada
- API Key do Firebase

### Passo a passo

1. Clone o repositório:
```bash
git clone https://github.com/seuusuario/poupae.git
```

2. Abra no Android Studio.

3. Configure o Firebase:
   - Crie um projeto no [Firebase Console](https://console.firebase.google.com/).
   - Adicione um novo app Android.
   - Baixe o arquivo `google-services.json`.
   - Coloque o arquivo em `app/`.

4. Sincronize com Gradle e execute o projeto.

---

## 🧱 Arquitetura e Tecnologias

### Arquitetura
- **MVVM (Model-View-ViewModel)**: separação clara entre lógica de negócios e interface.
- `ViewModel` para gerenciamento de dados.
- Firebase Firestore encapsulado nos `ViewModels`.

### Tecnologias Utilizadas
- **Kotlin**
- **Firebase Firestore** (banco de dados)
- **Firebase Authentication** (login com Google)
- **Material Design Components**
- **RecyclerView**, **DialogFragment**, **BottomNavigationView**
- **LiveData** e **ViewModel**
- **Gráficos** (ex: MPAndroidChart, se utilizado)

---

## 📌 Autores

- Desenvolvido por Kauan Batista dos Santos (@seuusuario)

---

## 📃 Licença

Este projeto está sob a licença MIT - veja o arquivo LICENSE para detalhes.