import { createWebHistory, createRouter, RouteRecordRaw } from "vue-router";
import HomeView from "./components/HomeView.vue";
import AboutView from "./components/AboutView.vue";
import BookView from "./components/BookView.vue";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/home",
    name: "Home",
    component: HomeView
  },
  {
    path: "/about",
    name: "About",
    component: AboutView
  },
  {
    path: "/book",
    name: "Book",
    component: BookView,
    props: true
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
  },
];


export const router = createRouter({
  history: createWebHistory(),
  routes
});